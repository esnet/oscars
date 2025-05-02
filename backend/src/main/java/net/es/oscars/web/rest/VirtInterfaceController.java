package net.es.oscars.web.rest;

import java.util.*;

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
import net.es.oscars.web.beans.VirtIpInterfaceRequest;
import net.es.oscars.web.beans.VirtIpInterfaceResponse;

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

        String ipAddress = checkIpAddressAndSubnetFormat(ipAndSubnet);

        Set<NsoVirtInterface> interfaces = virtInterfaceDAO.findByConnectionId(connectionId);

        // if no instances exist: create first one for circuit and add virt ip
        if (interfaces == null || interfaces.isEmpty()) {
            log.info("No existing instances for connection id" + connectionId + " found");
            NsoVirtInterface newEntry = new NsoVirtInterface();
            newEntry.setDevice(device);
            newEntry.setConnectionId(connectionId);
            newEntry.getIpAddresses().add(ipAndSubnet);
            virtInterfaceDAO.save(newEntry);
            log.info("Virt IP " + ipAndSubnet + " added to " + device + " for connection id " + connectionId);
            return;
        }

        // if entries exist:
        // 1. check all entries if ip/net or ip already exists
        // 2. if yes -> error out | if no -> create / add entry
        boolean ipExists = false;
        HashMap<String, NsoVirtInterface> cache = new HashMap<String, NsoVirtInterface>();
        for (NsoVirtInterface ifce : interfaces) {
            cache.put(ifce.getDevice(), ifce);
            String errorMsg = "Found identical IP " + ipAddress + " for device " + ifce.getDevice() + " for connection id " + connectionId;
            if (ifce.getIpAddresses().contains(ipAndSubnet)) {
                // check set for CIDR address - this should mostly work in case the address exists already
                ipExists = true;
                log.info(errorMsg);
            } else {
                // check again for IP address only - makes sure that there is no duplicate with another netmask /16 <=> /24
                for (String existingIp : ifce.getIpAddresses()) {
                    String existingIpAddress = checkIpAddressAndSubnetFormat(existingIp);
                    if (ipAddress.equals(existingIpAddress)) {
                        ipExists = true;
                        log.info(errorMsg);
                        break; // ip match found no further search required
                    }
                }
            }
        }
        // if virt ip doesn't exist -> add virt ip
        NsoVirtInterface instanceToModify = null;
        if (!ipExists) {
            instanceToModify = cache.get(device);

            // if no entry for this devices exists -> create new entry
            if (instanceToModify == null) {
                instanceToModify = new NsoVirtInterface();
                instanceToModify.setDevice(device);
                instanceToModify.setConnectionId(connectionId);
            }

            // add virt ip
            instanceToModify.getIpAddresses().add(ipAndSubnet);

            // add to DB
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

        Set<NsoVirtInterface> interfaces = virtInterfaceDAO.findByConnectionId(connectionId);
        if(interfaces == null || interfaces.isEmpty()) {
            log.info("No virt IPs found");
            throw new NoSuchElementException();
        }

        boolean virtIpFound = false;
        for (NsoVirtInterface ifce : interfaces) {
            if (ifce.getIpAddresses().contains(ipAndSubnet)) {
                virtIpFound = true;

                // if last ip address remove entry form DB else modify IP address list
                if (ifce.getIpAddresses().size() <= 1) {
                    virtInterfaceDAO.deleteById(ifce.getId());
                } else {
                    ifce.getIpAddresses().remove(ipAndSubnet);
                    virtInterfaceDAO.save(ifce);
                }

                log.info("Virt IP " + ipAndSubnet + " removed from " + device + " with connection id " + connectionId);
                break;
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
    public List<VirtIpInterfaceResponse> listVirtIpInterface(@PathVariable String connectionId) {

        if (connectionId == null) {
            log.info("REST request connection id was null");
            throw new NoSuchElementException();
        }

        Connection conn = connSvc.findConnection(connectionId).orElseThrow(NoSuchElementException::new);

        Set<NsoVirtInterface> interfaces = virtInterfaceDAO.findByConnectionId(connectionId);

        List<VirtIpInterfaceResponse> response = new ArrayList<VirtIpInterfaceResponse>();
        for (NsoVirtInterface ifce : interfaces) {
            VirtIpInterfaceResponse entry = new VirtIpInterfaceResponse();
            entry.setDevice(ifce.getDevice());
            entry.setIpInterfaces(new ArrayList<>(ifce.getIpAddresses()));
            response.add(entry);
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
        Connection conn = connSvc.findConnection(connectionId).orElseThrow(NoSuchElementException::new);

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
        if (checkIpAddressAndSubnetFormat(ipAndSubnet) == null) {
            log.info("The provided IP/SUBNET info " + ipAndSubnet + " is invalid");
            throw new IllegalArgumentException();
        }
    }

    /**
     * Checks for the correct IP/SUBNET notation in CIDR format, e.g. 10.0.0.1/24
     * The supported subnet range is 8 - 32
     * @param ipAndSubnet String with IP/SUBNET address in CIDR format
     * @return the IP address as a string or null if the format is not correct
     */
    public String checkIpAddressAndSubnetFormat(String ipAndSubnet) {
        if(ipAndSubnet == null) return null;

        String[] ipAndNetmask = ipAndSubnet.split("/");
        if(ipAndNetmask.length != 2) return null;

        if(!InetAddresses.isInetAddress(ipAndNetmask[0])) return null;

        int netmask = -1;
        try {
             netmask = Integer.parseInt(ipAndNetmask[1]);
        } catch (NumberFormatException error) {
            log.info("Error paring netmask");
            return null;
        }
        if(netmask < 8 || netmask > 32) return null; // this matches the NSO yang validation

        return ipAndNetmask[0];
    }

}
