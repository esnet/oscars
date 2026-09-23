package net.es.oscars.topo.svc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.es.oscars.esdb.EsdbCache;
import net.es.oscars.sb.nso.cache.NsoServiceConfigCache;
import net.es.oscars.topo.beans.CustomBandwidth;
import net.es.topo.common.dto.esdb.EsdbEqIfceBw;
import net.es.topo.common.dto.esdb.EsdbEquip;
import net.es.topo.common.dto.esdb.EsdbVlanWithDetails;
import net.es.topo.common.dto.nso.*;
import net.es.topo.common.dto.nso.enums.NsoRole;
import net.es.topo.common.model.oscars1.OscarsOneAdjcy;
import net.es.topo.common.model.oscars1.OscarsOneDevice;
import net.es.topo.common.model.oscars1.OscarsOneTopo;
import net.es.topo.common.model.oscars1.Problem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TopoGenerator {
    private final EsdbCache esdbCache;
    private final NsoServiceConfigCache serviceConfigCache;
    private final ServiceToOscars serviceToOscars;

    @Value("file:config/custom-bw.json")
    protected Resource customBwResource;

    public TopoGenerator(EsdbCache esdbCache, NsoServiceConfigCache serviceConfigCache, ServiceToOscars serviceToOscars) {
        this.esdbCache = esdbCache;
        this.serviceConfigCache = serviceConfigCache;
        this.serviceToOscars = serviceToOscars;
    }


    @Cacheable(value = "topology", key = "#root.methodName")
    public OscarsOneTopo generate() throws IOException {

        List<OscarsOneDevice> oscarsDevices = new ArrayList<>();

        SharedPrep sharedPrep = this.prepare();

        Map<String, NsoSystem> systemsByName = sharedPrep.systems.stream().collect(Collectors.toMap( NsoSystem::getDevice, Function.identity()));
        List<String> coreRouters = new ArrayList<>();
        for (FromNsoDevice nsoDevice : sharedPrep.dl().getDevices()) {
            // this ensures we retrieve an empty list instead of a null
            if (!sharedPrep.vlansByDevice().containsKey(nsoDevice.getName())) {
                sharedPrep.vlansByDevice().put(nsoDevice.getName(), new ArrayList<>());
            }

            if (nsoDevice.getRole().equals(NsoRole.CORE_ROUTER)) {
                coreRouters.add(nsoDevice.getName());
            }
        }

        List<Problem> problems = new ArrayList<>();

        List<OscarsOneAdjcy> adjcies = this.serviceToOscars.makeAdjacencies(sharedPrep.bbls, coreRouters);
        Set<String> mplsPorts = new HashSet<>();
        for (OscarsOneAdjcy adjcy : adjcies) {
            mplsPorts.add(adjcy.getA().getPort());
            mplsPorts.add(adjcy.getZ().getPort());
        }


        for (FromNsoDevice nsoDevice : sharedPrep.dl().getDevices()) {
            if (nsoDevice.getRole().equals(NsoRole.CORE_ROUTER)) {
                EsdbEquip equip = sharedPrep.equipByName().get(nsoDevice.getName());
                OscarsOneDevice od = this.serviceToOscars.makeDevice(
                        nsoDevice.getName(),
                        equip,
                        sharedPrep.vlansByDevice.get(nsoDevice.getName()),
                        sharedPrep.ports,
                        systemsByName.get(nsoDevice.getName()),
                        mplsPorts);

                if (od != null) {
                    oscarsDevices.add(od);
                }
            }
        }

        this.logProblems(problems);

        return OscarsOneTopo.builder()
                .devices(oscarsDevices)
                .problems(problems)
                .adjcies(adjcies)
                .build();

    }

    private SharedPrep prepare() throws IOException {
        esdbCache.evictAllData();

        List<EsdbEquip> equips = esdbCache.getEquip();
        List<EsdbVlanWithDetails> vlans = esdbCache.getVlanWithDetails();
        List<EsdbEqIfceBw> eqIfceBws = esdbCache.getEqIfceBw();

        serviceConfigCache.evictAllServiceConfigs();

        // get NSO service configs
        List<NsoPort> ports = serviceConfigCache.getPort().getNsoPorts();
        List<NsoSystem> systems = serviceConfigCache.getSystem().getNsoSystems();
        List<NsoBBL> bbls = serviceConfigCache.getBBL().getNsoBbls();
        FromNsoDeviceList dl = serviceConfigCache.getDeviceList();


        File cbFile = customBwResource.getFile();
        String content = new String(Files.readAllBytes(cbFile.toPath()));

        ObjectMapper mapper = new ObjectMapper();
        CustomBandwidth[] cbArray = mapper.readValue(content, CustomBandwidth[].class);
        List<CustomBandwidth> customBws = new ArrayList<>(Arrays.asList(cbArray));

        Map<Integer, Integer> eqIfceSpeeds = eqIfceBws.stream()
                .collect(Collectors.toMap(EsdbEqIfceBw::getId, EsdbEqIfceBw::getSpeed));

        for (EsdbEquip eq : equips) {
            for (EsdbEquip.EsdbEquipmentIfceInline eqIfce : eq.getIfces()) {
                if (eqIfce.getIfceBw() != null) {
                    if (eqIfceSpeeds.containsKey(eqIfce.getIfceBw())) {
                        eqIfce.setSpeed(eqIfceSpeeds.get(eqIfce.getIfceBw()));
                    }
                }
                for (CustomBandwidth cbw : customBws) {
                    if (eq.getName().equals(cbw.getDevice()) && eqIfce.getIfce().equals(cbw.getPort())) {
                        int prevSpeed = eqIfce.getSpeed();
                        eqIfce.setSpeed(cbw.getMbps());
                        log.info("overriding "+prevSpeed+" bandwidth for "+eq.getName()+":"+eqIfce.getIfce()+" - now "+eqIfce.getSpeed());
                    }
                }
                // be able to look up ifce by name
                eq.getIfcesByName().put(eqIfce.getIfce(), eqIfce);
            }
        }

        // we will need to look up equip by name
        Map<String, EsdbEquip> equipByName = equips.stream()
                .collect(Collectors.toMap(EsdbEquip::getName, Function.identity()));

        Map<String, List<EsdbVlanWithDetails>> vlansByDevice = new HashMap<>();
        for (EsdbVlanWithDetails vlan : vlans) {
            String device = vlan.getEquipment().getName();
            if (!vlansByDevice.containsKey(device)) {
                vlansByDevice.put(device, new ArrayList<>());
            }
            vlansByDevice.get(device).add(vlan);
        }



        return new SharedPrep(ports, systems, bbls, equipByName, vlansByDevice, dl);
    }

    public void logProblems(List<Problem> problems) {
        for (Problem p : problems) {
            switch (p.getSeverity()) {
                case FATAL -> log.error("%s : FATAL : %s".formatted(p.getDevice(), p.getDescription()));
                case MAJOR -> log.warn("%s : MAJOR : %s".formatted(p.getDevice(), p.getDescription()));
                case MINOR -> log.info("%s : MINOR : %s".formatted(p.getDevice(), p.getDescription()));
                case DEBUG -> log.debug("%s : DEBUG : %s".formatted(p.getDevice(), p.getDescription()));
            }
        }
    }


    private record SharedPrep(List<NsoPort> ports,
                              List<NsoSystem> systems,
                              List<NsoBBL> bbls,
                              Map<String, EsdbEquip> equipByName,
                              Map<String, List<EsdbVlanWithDetails>> vlansByDevice,
                              FromNsoDeviceList dl) {
    }

    public static Integer getReservableBw(Integer physicalSpeed, EsdbEquip.EsdbEquipmentIfceInline esdbEqIfce) {
        Integer reservableBw = physicalSpeed;

        // we ignore missing values and negatives - but you can go over 100%
        if (esdbEqIfce.getOscarsBwPercent() != null) {
            if  (esdbEqIfce.getOscarsBwPercent() < 0 || esdbEqIfce.getOscarsBwPercent() > 100) {
                log.warn("oscars bandwidth for ESDB equip ifce id: "+esdbEqIfce.getId()+" outside acceptable range [0-100] ");

            } else {
                reservableBw = physicalSpeed * esdbEqIfce.getOscarsBwPercent() / 100;
            }
        }
        return reservableBw;
    }
}
