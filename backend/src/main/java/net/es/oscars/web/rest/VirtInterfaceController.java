package net.es.oscars.web.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.net.InetAddresses;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.resv.ent.Connection;
import net.es.oscars.resv.ent.VlanFixture;
import net.es.oscars.resv.svc.ConnService;
import net.es.oscars.sb.nso.db.NsoVirtInterfaceDAO;
import net.es.oscars.sb.nso.ent.NsoVirtInterface;
import net.es.oscars.web.beans.VirtIpInterfaceListResponse;
import net.es.oscars.web.beans.VirtIpInterfaceRequest;

@Slf4j
@RestController
public class VirtInterfaceController {

    private final NsoVirtInterfaceDAO virtInterfaceDAO;
    private final ConnService connSvc;


    public VirtInterfaceController(NsoVirtInterfaceDAO virtInterfaceDAO,
                                    ConnService connSvc) {
        this.virtInterfaceDAO = virtInterfaceDAO;
        this.connSvc = connSvc;
    }


    @RequestMapping(value = "/api/interface/add", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public void addVirtIpInterface(@RequestBody VirtIpInterfaceRequest request) {
        log.debug("request: " + request.toString());

        verifyData(request);

        String connectionId = request.getConnectionId();
        String device = request.getDevice();
        String ipAndSubnet = request.getIpAndSubnet();

        List<NsoVirtInterface> interfaces = virtInterfaceDAO.findByConnectionId(connectionId);

        // if no instances exist: create first one for circuit and add virt ip
        if (interfaces == null || interfaces.isEmpty()) {
            NsoVirtInterface newEntry = new NsoVirtInterface();
            newEntry.setDevice(device);
            newEntry.setConnectionId(connectionId);
            newEntry.setIpAddresses(new ArrayList<>());
            newEntry.getIpAddresses().add(ipAndSubnet);
            virtInterfaceDAO.save(newEntry);
            log.info("Virt IP " + ipAndSubnet + " added to " + device + " for connection id " + connectionId);
            return;
        }

        // if entries exist:
        // 1. check all entries if ip/net already exists
        // 2. if yes -> error out | if no -> create / add entry
        boolean ipExists = false;
        HashMap<String, NsoVirtInterface> cache = new HashMap<String, NsoVirtInterface>();
        for (NsoVirtInterface ifce : interfaces) {
            cache.put(ifce.getDevice(), ifce);
            for (String existingIp : ifce.getIpAddresses()) {
                if (existingIp.equals(ipAndSubnet)) {
                    ipExists = true;
                    log.info("Found existing IP");
                }
            }
        }
        log.info("checked all ips");
        // if virt ip doesn't exist -> add virt ip
        NsoVirtInterface instanceToModify = null;
        if (!ipExists) {
            instanceToModify = cache.get(device);

            // if no entry for this devices exists -> create new entry
            if (instanceToModify == null) {
                instanceToModify = new NsoVirtInterface();
            }

            // if no list exists for the device entry -> create new virt ip list
            if (instanceToModify.getIpAddresses() == null)  {
                instanceToModify.setIpAddresses(new ArrayList<String>());
            }

            // add virt ip
            instanceToModify.getIpAddresses().add(ipAndSubnet);

            virtInterfaceDAO.save(instanceToModify);

            log.info("Virt IP " + ipAndSubnet + " added to " + device + " for connection id " + connectionId);
        } else {
            log.info("Virt IP " + ipAndSubnet + " already exists");
            throw new IllegalArgumentException();
        }
        return;
    }


    @RequestMapping(value = "/api/interface/remove", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public void removeVirtIpInterface(@RequestBody VirtIpInterfaceRequest request) {
        log.debug("request: " + request.toString());

        verifyData(request);

        String connectionId = request.getConnectionId();
        String device = request.getDevice();
        String ipAndSubnet = request.getIpAndSubnet();

        List<NsoVirtInterface> interfaces = virtInterfaceDAO.findByConnectionId(connectionId);
        if(interfaces == null || interfaces.isEmpty()) {
            log.info("No virt IPs found");
            throw new NoSuchElementException();
        }

        boolean virtIpFound = false;
        for (NsoVirtInterface ifce : interfaces) {
            for (String ipAddress : ifce.getIpAddresses()) {
                if(ipAndSubnet.equals(ipAddress)) {
                    virtIpFound = true;
                    virtInterfaceDAO.deleteById(ifce.getId());
                    log.info("Virt IP " + ipAndSubnet + " removed from " + device + " with connection id " + connectionId);
                }
            }
        }

        if(!virtIpFound) {
            log.info("No virt IP " + ipAndSubnet + " found for connection id " + connectionId);
            throw new NoSuchElementException();
        }
    }


    @RequestMapping(value = "/api/interface/list/{connectionId}", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public VirtIpInterfaceListResponse listVirtIpInterface(@PathVariable String connectionId) {

        if (connectionId == null) {
            log.info("REST request connection id was null");
            throw new NoSuchElementException();
        }

        Connection conn = connSvc.findConnection(connectionId);
        if (conn == null) {
            log.info("Couldn't find OSCARS circuit for connection id " + connectionId);
            throw new NoSuchElementException();
        }

        List<NsoVirtInterface> interfaces = virtInterfaceDAO.findByConnectionId(connectionId);
        if (interfaces == null) {
            log.info("No virtual IP entries found for connection id " + connectionId);
            throw new NoSuchElementException();
        }

        VirtIpInterfaceListResponse response = new VirtIpInterfaceListResponse();
        for (NsoVirtInterface ifce : interfaces) {
            VirtIpInterfaceListResponse.ListEntry entry = new VirtIpInterfaceListResponse.ListEntry();
            entry.setDevice(ifce.getDevice());
            entry.setIpInterfaces(ifce.getIpAddresses());
            response.getList().add(entry);
        }
        return response;
    }

    /**
     * Validates if the request data is valid and throws an NoSuchElementException if
     * something is missing.
     * @param request REST request
     * @throws NoSuchElementException if any input parameter is null
     */
    private void checkRequestData(VirtIpInterfaceRequest request) throws NoSuchElementException {
        if(request == null) {
            log.info("Couldn't extract REST request data");
            throw new NoSuchElementException();
        }
        if(request.getConnectionId() == null) {
            log.info("No connection id found in REST request");
            throw new NoSuchElementException();
        }
        if(request.getDevice() == null) {
            log.info("No device id found in REST request");
            throw new NoSuchElementException();
        }
        if(request.getIpAndSubnet() == null) {
            log.info("No ip/netmask info found in REST request");
            throw new NoSuchElementException();
        }
    }

    /**
     * Verifies that the request data is valid and the it corresponds with existing OSCARS
     * data / connection id exists, device is part of circuit.
     * Throws an exception if the checks are not successful.
     * @param request the REST request
     * @throws NoSuchElementException if parameter is missing
     * @throws IllegalArgumentException if parameter is not well formed
     */
    private void verifyData(VirtIpInterfaceRequest request) throws NoSuchElementException, IllegalArgumentException {
        checkRequestData(request);

        String connectionId = request.getConnectionId();
        String device = request.getDevice();
        String ipAndSubnet = request.getIpAndSubnet();

        // check for connection id and find devices in circuit
        Connection conn = connSvc.findConnection(connectionId);
        if (conn == null) {
            log.info("Couldn't find OSCARS circuit for connection id " + connectionId);
            throw new NoSuchElementException();
        }

        // check if device exists
        boolean deviceFound = false;
        for (VlanFixture f : conn.getReserved().getCmp().getFixtures()) {
            String deviceUrn = f.getJunction().getDeviceUrn();
            if (deviceUrn.equals(device)) {
                deviceFound = true;
                break;
            }
        }
        if (!deviceFound) {
            log.info("Couldn't find device for connection id " + connectionId);
            throw new NoSuchElementException();
        }

        // check ip address format
        if (!checkIpAddressAndSubnetFormat(ipAndSubnet)) {
            log.info("The provided IP/SUBNET info " + ipAndSubnet + " is invalid");
            throw new IllegalArgumentException();
        }
    }


    /**
     * Checks for the correct IP/SUBNET notation in CIDR format, e.g. 10.0.0.1/24
     * The supported subnet range is 8 - 32
     * @param ipAndSubnet String with IP/SUBNET address in CIDR format
     * @return true if string format is correct, otherwise false
     */
    public boolean checkIpAddressAndSubnetFormat(String ipAndSubnet) {
        if(ipAndSubnet == null) return false;

        String[] ipAndNetmask = ipAndSubnet.split("/");
        if(ipAndNetmask.length != 2) return false;

        if(!InetAddresses.isInetAddress(ipAndNetmask[0])) return false;

        int netmask = -1;
        try {
             netmask = Integer.parseInt(ipAndNetmask[1]);
        } catch (NumberFormatException error) {
            log.info("Error paring netmask");
            return false;
        }
        if(netmask < 8 || netmask > 32) return false; // this matches the NSO validation

        return true;
    }

}
