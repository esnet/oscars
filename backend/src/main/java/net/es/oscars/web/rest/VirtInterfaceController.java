package net.es.oscars.web.rest;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
    public void addVirtIpInterface(VirtIpInterfaceRequest request) {

        checkRequestData(request);

        String connectionId = request.getConnectionId();
        String device = request.getDevice();
        String ipAndSubnet = request.getIpAndSubnet();

        // heck for connection id and find devices in circuit
        Connection conn = connSvc.findConnection(connectionId);
        if (conn == null) {
            log.info("Couldn't find OSCARS circuit for connection id " + connectionId);
            throw new NoSuchElementException();
        }

        // check if device exists
        boolean deviceFound = false;
        for (VlanFixture f : conn.getReserved().getCmp().getFixtures()) {
            String deviceUrn = f.getJunction().getDeviceUrn();
            if(deviceUrn.equals(device)) {
                deviceFound = true;
                break;
            }
        }
        if(!deviceFound) {
            log.info("Couldn't find device for connection id " + connectionId);
            throw new NoSuchElementException();
        }

        // check ip address format
        List<NsoVirtInterface> interfaces = virtInterfaceDAO.findNsoVirtInterfaceByConnectionId(connectionId);

        if(interfaces == null) {
            // create and add
        } else {
            // add
            boolean addedInterfaceToExistingEntry = false;
            for(NsoVirtInterface ifce : interfaces) {

                if(device.equals(ifce.getDevice())) {


                    boolean ipExists = false;
                    for(String existingIp : ifce.getIpAddresses()) {

                        if(existingIp.equals(ipAndSubnet)) {
                            log.info("Virt IP " + ipAndSubnet + " already exists");
                            // throw exception!?
                        }

                    } // for
                    if(!ipExists) {
                        ifce.getIpAddresses().add(ipAndSubnet);
                        virtInterfaceDAO.save(ifce);
                        log.info("Virt IP " + ipAndSubnet + " added to " + device + " for connection id " + connectionId);
                    }

                } // if

            } // for
        } // if - else

        // virtInterfaceDAO.save();

    }


    @RequestMapping(value = "/api/interface/remove", method = RequestMethod.POST)
    @ResponseBody
    @Transactional
    public void removeVirtIpInterface(VirtIpInterfaceRequest request) {

        checkRequestData(request);

    }


    @RequestMapping(value = "/api/interface/list", method = RequestMethod.GET)
    @ResponseBody
    @Transactional
    public VirtIpInterfaceListResponse listVirtIpInterface() {
        return null;
    }


    private void checkRequestData(VirtIpInterfaceRequest request) {
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

}
