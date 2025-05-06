package net.es.oscars.sb.nso;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoStateManagerException;
import net.es.topo.common.dto.nso.NsoLSP;
import net.es.topo.common.dto.nso.NsoVPLS;
import net.es.topo.common.dto.nso.enums.NsoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * # Rules for LSPs. (@haniotak)
 *   #
 *   # - An LSP is associated with exactly one VPLS ; a VPLS will have multiple LSPs
 *   # - No LSPs should be present in NSO that are not associated with a VPLS ("orphans")
 *   #
 *   # DELETE
 *   # - When a VPLS is deleted, all LSPs associated with it should be deleted
 *   # - To delete an LSP associated with a VPLS, the VPLS must be deleted first
 *   # - You can always delete an "orphan" LSP if you find one
 *   #
 *   # ADD
 *   # - You must add all LSPs before adding their associated VPLS
 *   # - Do not add any "orphan" LSPs
 *   #
 *   # REDEPLOY
 *   # - Redeploying a VPLS can mean it is associated with a new set of VPLSs  - i.e. from {A, B, C} to {B, C', D} (C' has changed and needs to be redeployed)
 *   # - Add any new LSPs first (D), redeploy any LSPs that changed (C), redeploy the VPLS itself, then delete the newly orphan LSPs (A)
 */
@Getter
@Setter
@Slf4j
@Component
public class NsoStateManager {
    @Autowired
    private NsoVplsStateSyncer nsoVplsStateSyncer;
    @Autowired
    private NsoLspStateSyncer nsoLspStateSyncer;

    private boolean isValid = false;
    private boolean isQueued = false;
    private boolean isVplsSynced = false;
    private boolean isLspSynced = false;

    /**
     * Validate the combined VPLS and LPS state.
     * @return Returns true if validation passed, false otherwise.
     * @throws NsoStateManagerException Throws an exception if validation encounters an unexpected error.
     */
    public boolean validate() throws NsoStateManagerException {
        try {
            this.setValid(false);

            // For each VPLS, assert we have an entry for the expected LSP
            // - An LSP is associated with exactly one VPLS ; a VPLS will have multiple LSPs
            // - No LSPs should be present in NSO that are not associated with a VPLS ("orphans")
            boolean isValid = true;

            Dictionary<Integer, NsoStateWrapper<NsoVPLS>> nsoVplsLocal = nsoVplsStateSyncer.getLocalState();
            Dictionary<Integer, NsoStateWrapper<NsoLSP>> nsoLspLocal = nsoLspStateSyncer.getLocalState();

            Enumeration<NsoStateWrapper<NsoLSP>> enumerationLsp = nsoLspStateSyncer.getLocalState().elements();
            Enumeration<NsoStateWrapper<NsoVPLS>> enumerationVpls = nsoVplsStateSyncer.getLocalState().elements();

            boolean isEntryValid = false;
            // Check LSPs exist for each VPLS first.
            while (enumerationLsp.hasMoreElements()) {
                NsoStateWrapper<NsoLSP> lspWrapped = enumerationLsp.nextElement();
                isEntryValid = _validateOneLspToOneVpls(lspWrapped.getInstance(), nsoVplsLocal);
                isValid = isValid && isEntryValid;
            }

            while (enumerationVpls.hasMoreElements()) {
                NsoStateWrapper<NsoVPLS> vplsWrapped = enumerationVpls.nextElement();
                isEntryValid = _validateOneVplsToOneLsp(vplsWrapped.getInstance(), nsoLspLocal);
                isValid = isValid && isEntryValid;
            }

            this.setValid(isValid);
        } catch (Exception ex) {
            log.error(ex.getLocalizedMessage(), ex);
            throw new NsoStateManagerException(ex.getLocalizedMessage());
        }
        return this.isValid();
    }

    private boolean _validateOneVplsToOneLsp(NsoVPLS vpls, Dictionary<Integer, NsoStateWrapper<NsoLSP>> nsoLspState) throws NsoStateManagerException {
        return _countVplsReferencesInLspState(vpls, nsoLspState) == 1;
    }

    private boolean _validateOneLspToOneVpls(NsoLSP lsp, Dictionary<Integer, NsoStateWrapper<NsoVPLS>> nsoVplsState) throws NsoStateManagerException {
        return _countLspReferencesInVplsState(lsp, nsoVplsState) == 1;
    }

    private int _countVplsReferencesInLspState(NsoVPLS vpls, Dictionary<Integer, NsoStateWrapper<NsoLSP>> nsoLspState) throws NsoStateManagerException {
        int timesFound = 0;

        List<NsoStateWrapper<NsoLSP>> vplsReferencesInLspState = _findVplsReferencesInLspState(vpls, nsoLspState);
        for (NsoStateWrapper<NsoLSP> wrappedNsoLSP : vplsReferencesInLspState) {
            NsoLSP lsp = wrappedNsoLSP.getInstance();

            for (NsoVPLS.SDP sdp : vpls.getSdp()) {
                if (sdp.getA().getLsp().equals(lsp.getName())) {
                    timesFound++;
                }

                if (sdp.getZ().getLsp().equals(lsp.getName())) {
                    timesFound++;
                }
            }
        }

        return timesFound;
    }

    private int _countLspReferencesInVplsState(NsoLSP lsp, Dictionary<Integer, NsoStateWrapper<NsoVPLS>> nsoVplsState) throws NsoStateManagerException {
        int timesFound = 0;

        List<NsoStateWrapper<NsoVPLS>> lspReferencesInVplsState = _findLspReferencesInVplsState(lsp, nsoVplsState);
        for (NsoStateWrapper<NsoVPLS> wrappedNsoVPLS : lspReferencesInVplsState) {
            NsoVPLS vpls = wrappedNsoVPLS.getInstance();
            // Each VPLS has a lis of SDPs
            for (NsoVPLS.SDP sdp : vpls.getSdp()) {
                // Each SDP contains an entry A, and an entry Z, which represent LSP A and LSP Z respectively.
                // One (1) LSP should be associated with one (1) VPLS, as either A or Z, but not both.
                String lspA = sdp.getA().getLsp();
                String lspZ = sdp.getZ().getLsp();

                if (lsp.getName().equals(lspA)) {
                    timesFound += 1;
                }
                if (lsp.getName().equals(lspZ)) {
                    timesFound += 1;
                }
            }
        }

        return timesFound;
    }

    private List<NsoStateWrapper<NsoVPLS>> _findLspReferencesInVplsState(NsoLSP lsp, Dictionary<Integer, NsoStateWrapper<NsoVPLS>> nsoVplsState) throws NsoStateManagerException {
        Enumeration<NsoStateWrapper<NsoVPLS>> enumeration = nsoVplsState.elements();
        List<NsoStateWrapper<NsoVPLS>> lspReferencesInVplsState = new ArrayList<>();

        while (enumeration.hasMoreElements()) {
            NsoStateWrapper<NsoVPLS> wrappedNsoVPLS = enumeration.nextElement();
            NsoVPLS vpls = wrappedNsoVPLS.getInstance();
            // Each VPLS has a lis of SDPs
            for (NsoVPLS.SDP sdp : vpls.getSdp()) {
                // Each SDP contains an entry A, and an entry Z, which represent LSP A and LSP Z respectively.
                // One (1) LSP should be associated with one (1) VPLS, as either A or Z, but not both.
                String lspA = sdp.getA().getLsp();
                String lspZ = sdp.getZ().getLsp();

                if (lsp.getName().equals(lspA) || lsp.getName().equals(lspZ)) {
                    lspReferencesInVplsState.add(wrappedNsoVPLS);
                }
            }
        }

        return lspReferencesInVplsState;
    }

    private List<NsoStateWrapper<NsoLSP>> _findVplsReferencesInLspState(NsoVPLS vpls, Dictionary<Integer, NsoStateWrapper<NsoLSP>> nsoLspState) throws NsoStateManagerException {
        Enumeration<NsoStateWrapper<NsoLSP>> enumeration = nsoLspState.elements();
        List<NsoStateWrapper<NsoLSP>> vplsReferencesInLspState = new ArrayList<>();

        while (enumeration.hasMoreElements()) {
            NsoStateWrapper<NsoLSP> wrappedNsoLsp = enumeration.nextElement();
            NsoLSP lsp = wrappedNsoLsp.getInstance();
            for (NsoVPLS.SDP sdp : vpls.getSdp()) {
                if (sdp.getA().getLsp().equals(lsp.getName()) || sdp.getZ().getLsp().equals(lsp.getName())) {
                    vplsReferencesInLspState.add(wrappedNsoLsp);
                }
            }
        }

        return vplsReferencesInLspState;
    }

    public boolean queue() throws NsoStateManagerException {
        this.setQueued(false);

        // @TODO Implement

        return this.isQueued();
    }

    public boolean sync() throws NsoStateManagerException, Exception {
        this.setVplsSynced(false);
        this.setLspSynced(false);

        if (!validate()) throw new NsoStateManagerException("NSO State Manager sync() failed. Validation failed.");

        if (!queue()) throw new NsoStateManagerException("NSO State Manager sync() failed. Queue failed.");

        // Sync LSPs first
        this.setLspSynced(
            this.nsoLspStateSyncer.sync(
                this.nsoLspStateSyncer.getNsoProxy().getNsoServiceConfigRestPath(NsoService.LSP)
            )
        );

        // Then Sync VPLSs
        this.setVplsSynced(
            this.nsoVplsStateSyncer.sync(
                this.nsoVplsStateSyncer.getNsoProxy().getNsoServiceConfigRestPath(NsoService.VPLS)
            )
        );

        return this.isVplsSynced() && this.isLspSynced();
    }

}
