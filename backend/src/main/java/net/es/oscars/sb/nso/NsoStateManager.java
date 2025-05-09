package net.es.oscars.sb.nso;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.sb.nso.dto.NsoStateWrapper;
import net.es.oscars.sb.nso.exc.NsoStateManagerException;
import net.es.oscars.sb.nso.exc.NsoStateSyncerException;
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
    private NsoProxy proxy;

    @Autowired
    private NsoVplsStateSyncer nsoVplsStateSyncer;
    @Autowired
    private NsoLspStateSyncer nsoLspStateSyncer;

    private boolean isLoaded = false;
    private boolean isValid = false;
    private boolean isQueued = false;
    private boolean isVplsSynced = false;
    private boolean isLspSynced = false;
    private boolean validationIgnoreOrphanedLsps = false;

    public NsoStateManager() {
        nsoVplsStateSyncer = new NsoVplsStateSyncer(proxy);
        nsoLspStateSyncer = new NsoLspStateSyncer(proxy);
    }

    public boolean load() throws NsoStateManagerException
    {
        try {
            setLoaded(
                nsoVplsStateSyncer.load()
                    && nsoLspStateSyncer.load()
            );
        } catch (NsoStateSyncerException e) {
            throw new NsoStateManagerException(e.getLocalizedMessage());
        }

        return isLoaded;
    }

    public void clear() {
        Dictionary<Integer, NsoStateWrapper<NsoVPLS>> clearVpls = new Hashtable<>();
        Dictionary<Integer, NsoStateWrapper<NsoLSP>> clearLsps = new Hashtable<>();

        nsoVplsStateSyncer.setLocalState(clearVpls);
        nsoLspStateSyncer.setLocalState(clearLsps);

        nsoVplsStateSyncer.setLoaded(false);
        nsoLspStateSyncer.setLoaded(false);

        nsoVplsStateSyncer.setSynchronized(false);
        nsoLspStateSyncer.setSynchronized(false);

        nsoVplsStateSyncer.setDirty(false);
        nsoLspStateSyncer.setDirty(false);

        setValid(false);
        setQueued(false);
        setVplsSynced(false);
        setLspSynced(false);
    }

    /**
     * Add or replace an LSP
     * @param lsp The NsoLSP to add, or the replacement NsoLSP if the LSP exists.
     * @throws NsoStateManagerException Throws an exception if there was an issue while attempting to mark the LSP for add or redeployment.
     */
    public void putLsp(NsoLSP lsp) throws NsoStateManagerException {
        Dictionary<Integer, NsoStateWrapper<NsoLSP>> localState = getNsoLspStateSyncer().getLocalState();
        NsoStateWrapper<NsoLSP> existingLsp = getNsoLspStateSyncer().findLocalEntryByName(lsp.instanceKey());

        // Add or replace the LSP.
        if (existingLsp != null) {
            localState.remove(existingLsp.getInstance().instanceKey().hashCode());
        }

        existingLsp = new NsoStateWrapper<>(NsoStateSyncer.State.NOOP, lsp);
        localState.put(lsp.instanceKey().hashCode(), existingLsp);
        getNsoLspStateSyncer().setLocalState(localState);
        getNsoLspStateSyncer().setDirty(true);
    }

    /**
     * Marks an LSP for deletion. If it was the last LSP associated with a VPLS, the VPLS is also marked for deletion.
     * @param lsp The NsoLSP to delete.
     * @return Returns true an existing LSP was found and marked for deletion.
     * @throws NsoStateManagerException Throws an exception if there was an issue while attempting to mark the LSP (and possible VPLS) for deletion.
     */
    public boolean deleteLsp(NsoLSP lsp) throws NsoStateManagerException {
        return deleteLsp(lsp, true);
    }
    /**
     * Marks an LSP for deletion. If it was the last LSP associated with a VPLS and deleteVPLSIfLast = true, the VPLS is also marked for deletion.
     * @param lsp The NsoLSP to delete.
     * @param deleteVPLSIfLast Boolean flag. If true, and the LSP was that last associated LSP with the corresponding VPLS, the VPLS will be marked for deletion.
     * @return Returns true an existing LSP was found and marked for deletion.
     * @throws NsoStateManagerException Throws an exception if there was an issue while attempting to mark the LSP (and possible VPLS) for deletion.
     */
    public boolean deleteLsp(NsoLSP lsp, boolean deleteVPLSIfLast) throws NsoStateManagerException {
        boolean success = false;
        NsoStateWrapper<NsoLSP> existingLsp = getNsoLspStateSyncer().findLocalEntryByName(lsp.instanceKey());

        // Only attempt to remove the LSP if it actually exists in the state.
        if (existingLsp != null) {


            getNsoLspStateSyncer().delete(existingLsp.getInstance().instanceKey().hashCode());

            // check if there are any LSPs left in the VPLS. If none, delete the VPLS
            Dictionary<Integer, NsoStateWrapper<NsoVPLS>> localVplsState = getNsoVplsStateSyncer().getLocalState();
            List<NsoStateWrapper<NsoVPLS>> existingVplsList = _findLspReferencesInVplsState(lsp, localVplsState);

            if (deleteVPLSIfLast) {

                for (NsoStateWrapper<NsoVPLS> wrappedVpls : existingVplsList) {
                    int countLspsInVpls = _countVplsReferencesInLspState(wrappedVpls.getInstance(), getNsoLspStateSyncer().getLocalState());

                    if (countLspsInVpls == 0) {
                        getNsoVplsStateSyncer().delete(wrappedVpls.getInstance().getVcId());
                        log.info("Deleted VPLS {}, as there are no more LSPs associated with it. ", wrappedVpls.getInstance().getName());
                    } else {
                        log.info("Will not delete VPLS. There are still {} LSP references in VPLS {}", countLspsInVpls, wrappedVpls.getInstance().getName());
                    }
                }
            }
            success = true;
        }

        return success;
    }

    /**
     * Add or replace a VPLS. The associated LSPs must already exist before attempting to add or replace the VPLS.
     * @param vpls The NsoVPLS to add, or the replacement NsoVPLS if the VPLS exists.
     * @throws NsoStateManagerException Throws an exception if there was an issue while attempting to mark the VPLS for add or redeployment.
     */
    public void putVpls(NsoVPLS vpls) throws NsoStateManagerException {
        Dictionary<Integer, NsoStateWrapper<NsoVPLS>> localState = getNsoVplsStateSyncer().getLocalState();
        NsoStateWrapper<NsoVPLS> existingVpls = getNsoVplsStateSyncer().findRemoteEntryById(vpls.getVcId());

        // The LSPs must already exist
        List<NsoStateWrapper<NsoLSP>> existingLsps = _findVplsReferencesInLspState(vpls, getNsoLspStateSyncer().getLocalState());

        if (existingLsps.isEmpty()) {
            throw new NsoStateManagerException("No LSP references found for " + vpls.getName() + " (" + vpls.getVcId() + "). Please check if the associated LSP references exist before adding or replacing the VPLS.");
        }

        // Add or replace the VPLS
        if (existingVpls != null) {
            localState.remove(existingVpls.getInstance().getVcId());
        }

        existingVpls = new NsoStateWrapper<>(NsoStateSyncer.State.NOOP, vpls);
        localState.put(existingVpls.getInstance().getVcId(), existingVpls);
        getNsoVplsStateSyncer().setLocalState(localState);
        getNsoVplsStateSyncer().setDirty(true);

    }

    /**
     * Marks a VPLS for deletion. Associated LSPs are marked for deletion.
     * Remember to call the sync() method to synchronize the NSO VPLS and LSP states.
     * @param vpls The NsoVPLS to delete.
     * @return Returns true if an existing VPLS was found and marked for deletion.
     * @throws NsoStateManagerException Throws an exception if there was an issue while attempting to mark the VPLS for removal.
     */
    public boolean deleteVpls(NsoVPLS vpls) throws NsoStateManagerException {
        boolean success = false;
//        Dictionary<Integer, NsoStateWrapper<NsoVPLS>> localVplsState = getNsoVplsStateSyncer().getLocalState();
//        Dictionary<Integer, NsoStateWrapper<NsoLSP>> localLspState = getNsoLspStateSyncer().getLocalState();

        NsoStateWrapper<NsoVPLS> existingVpls = getNsoVplsStateSyncer().findRemoteEntryById(vpls.getVcId());

        if (existingVpls != null) {
            // Delete the VPLS
//            localVplsState.remove(existingVpls.getInstance().getVcId());
            getNsoVplsStateSyncer().delete(existingVpls.getInstance().getVcId());

            // ...and delete associated the LSPs
            List<NsoStateWrapper<NsoLSP>> existingLspList = _findVplsReferencesInLspState(vpls, getNsoLspStateSyncer().getLocalState());
            for (NsoStateWrapper<NsoLSP> wrappedLsp : existingLspList) {
//                localLspState.remove(wrappedLsp.getInstance().instanceKey().hashCode());
                getNsoLspStateSyncer().delete(wrappedLsp.getInstance().instanceKey().hashCode());
            }

//            getNsoLspStateSyncer().setLocalState(localLspState);
//            getNsoVplsStateSyncer().setLocalState(localVplsState);
//
//            getNsoVplsStateSyncer().setDirty(true);
//            getNsoLspStateSyncer().setDirty(true);

            success = true;
        }

        return success;
    }


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
                if (lspWrapped.getState().equals(NsoStateSyncer.State.DELETE)) continue;

                isEntryValid = _validateOneLspToOneVpls(lspWrapped.getInstance(), nsoVplsLocal) || validationIgnoreOrphanedLsps;
                if (!isEntryValid) {
                    int count = _countLspReferencesInVplsState(lspWrapped.getInstance(), nsoVplsLocal);
                    if (count == 0) {
                        // This LSP isn't associated with a VPLS. It may become associated to a VPLS, or it may be orphaned.
                        log.warn("Found an LSP that does not belong to any VPLS. LSP instance: " + lspWrapped.getInstance().instanceKey());
                    } else {
                        log.warn("Found an LSP that belongs to more than one (1) VPLS. LSP instance: " + lspWrapped.getInstance().instanceKey());
                    }
                }
                isValid = isValid && isEntryValid;
            }

//            while (enumerationVpls.hasMoreElements()) {
//                NsoStateWrapper<NsoVPLS> vplsWrapped = enumerationVpls.nextElement();
//                isEntryValid = _validateOneVplsToOneLsp(vplsWrapped.getInstance(), nsoLspLocal);
//                if (!isEntryValid) {
//                    int count = _countVplsReferencesInLspState(vplsWrapped.getInstance(), nsoLspLocal);
//                    if (count == 0) {
//                        // This VPLS isn't associated with an LSP. It may need to be removed.
//                        log.warn("Found a VPLS that does not have any associated LSPs. VPLS name: " + vplsWrapped.getInstance().getName());
//                    } else {
//                        log.warn("Found a VPLS that is associated with more than one (1) LSP. VPLS name: " + vplsWrapped.getInstance().getName());
//                    }
//                }
//                isValid = isValid && isEntryValid;
//            }

            this.setValid(isValid);
        } catch (Exception ex) {
            log.error(ex.getLocalizedMessage(), ex);
            throw new NsoStateManagerException(ex.getLocalizedMessage());
        }
        return this.isValid();
    }

    public boolean validateVplsHasLspAssociation(NsoVPLS vpls, NsoLSP lsp, String asAorZ) throws NsoStateManagerException {
        boolean valid = false;

        if (!(asAorZ.equals("A") || asAorZ.equals("Z")) ) {
            throw new NsoStateManagerException("Invalid asAorZ string value. Must be \"A\" or \"Z\".");
        }

        for (NsoVPLS.SDP sdp : vpls.getSdp()) {
            switch (asAorZ) {
                case "A":
                    if (sdp.getA().getLsp().equals(lsp.getName()) && sdp.getA().getDevice().equals(lsp.getDevice()) ) {
                        valid = true;
                        break;
                    }
                    break;
                case "Z":
                    if (sdp.getZ().getLsp().equals(lsp.getName()) && sdp.getZ().getDevice().equals(lsp.getDevice()) ) {
                        valid = true;
                        break;
                    }
                    break;
            }
        }
        return valid;
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
            if (wrappedNsoLSP.getState().equals(NsoStateSyncer.State.DELETE)) continue;

            NsoLSP lsp = wrappedNsoLSP.getInstance();

            for (NsoVPLS.SDP sdp : vpls.getSdp()) {
                if (sdp.getA().getLsp().equals(lsp.getName()) && sdp.getA().getDevice().equals(lsp.getDevice()) ) {
                    timesFound++;
                }

                if (sdp.getZ().getLsp().equals(lsp.getName()) && sdp.getZ().getDevice().equals(lsp.getDevice()) ) {
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

            if (wrappedNsoVPLS.getState().equals(NsoStateSyncer.State.DELETE)) continue;

            NsoVPLS vpls = wrappedNsoVPLS.getInstance();
            // Each VPLS has a lis of SDPs
            for (NsoVPLS.SDP sdp : vpls.getSdp()) {
                // Each SDP contains an entry A, and an entry Z, which represent LSP A and LSP Z respectively.
                // One (1) LSP should be associated with one (1) VPLS, as either A or Z, but not both.

                if (sdp.getA().getLsp().equals(lsp.getName()) && sdp.getA().getDevice().equals(lsp.getDevice()) ) {
                    timesFound += 1;
                }
                if (sdp.getZ().getLsp().equals(lsp.getName()) && sdp.getZ().getDevice().equals(lsp.getDevice()) ) {
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
            if (wrappedNsoVPLS.getState().equals(NsoStateSyncer.State.DELETE)) continue;
            NsoVPLS vpls = wrappedNsoVPLS.getInstance();
            // Each VPLS has a lis of SDPs
            if (vpls.getSdp() == null || vpls.getSdp().isEmpty()) {
                continue;
            }
            for (NsoVPLS.SDP sdp : vpls.getSdp()) {
                // Each SDP contains an entry A, and an entry Z, which represent LSP A and LSP Z respectively.
                // One (1) LSP should be associated with one (1) VPLS, as either A or Z, but not both.
                if (
                    ( sdp.getA().getLsp().equals(lsp.getName()) && sdp.getA().getDevice().equals(lsp.getDevice()) )
                    || ( sdp.getZ().getLsp().equals(lsp.getName()) && sdp.getZ().getDevice().equals(lsp.getDevice()) )
                ) {
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
            if (wrappedNsoLsp.getState().equals(NsoStateSyncer.State.DELETE)) continue;

            NsoLSP lsp = wrappedNsoLsp.getInstance();
            if (vpls.getSdp() == null || vpls.getSdp().isEmpty()) {
                continue;
            }
            for (NsoVPLS.SDP sdp : vpls.getSdp()) {
                if (sdp.getA().equals(sdp.getZ())) {
                    throw new NsoStateManagerException("A VPLS should not re-use the same LSP within an SDP as both point A and Z");
                }
                if (
                    ( sdp.getA().getLsp().equals(lsp.getName()) && sdp.getA().getDevice().equals(lsp.getDevice()) )
                    || ( sdp.getZ().getLsp().equals(lsp.getName()) && sdp.getZ().getDevice().equals(lsp.getDevice()) )
                ) {
                    vplsReferencesInLspState.add(wrappedNsoLsp);
                }
            }
        }

        return vplsReferencesInLspState;
    }

//    public boolean queue() throws NsoStateManagerException {
//        this.setQueued(false);
//
//        // @TODO Implement
//
//        return this.isQueued();
//    }

    public boolean sync() throws NsoStateManagerException, Exception {
        this.setVplsSynced(false);
        this.setLspSynced(false);

        if (!validate()) throw new NsoStateManagerException("NSO State Manager sync() failed. Validation failed.");

//        if (!queue()) throw new NsoStateManagerException("NSO State Manager sync() failed. Queue failed.");

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
        log.info("Sync complete. VPLS is synchronized? ({}), LSP is sychronized? ({})", this.isVplsSynced(), this.isLspSynced());
        return this.isVplsSynced() && this.isLspSynced();
    }

}
