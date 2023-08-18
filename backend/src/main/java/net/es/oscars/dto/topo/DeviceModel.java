package net.es.oscars.dto.topo;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum DeviceModel {
    ALCATEL_SR7750("Alcatel SR-7750"),
    JUNIPER_MX("Juniper MX"),
    JUNIPER_EX("Juniper EX");

    private String code;

    DeviceModel(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }


    private static final Map<String, DeviceModel> lookup = new HashMap<String, DeviceModel>();

    static {
        for (DeviceModel pc : EnumSet.allOf(DeviceModel.class)) {
            lookup.put(pc.getCode(), pc);
        }
    }

    public static Optional<DeviceModel> get(String code) {
        Optional<DeviceModel> result;
        if (lookup.containsKey(code)) {
            return Optional.of(lookup.get(code));
        } else {
            return Optional.empty();
        }
    }
}
