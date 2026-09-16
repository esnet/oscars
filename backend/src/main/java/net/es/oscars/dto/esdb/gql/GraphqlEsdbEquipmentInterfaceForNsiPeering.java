package net.es.oscars.dto.esdb.gql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class GraphqlEsdbEquipmentInterfaceForNsiPeering {
    private String id;
    private DeviceContainer device;
    private String interfaceName;
    private Bandwidth interfaceBandwidth;

    public Integer getId() {
        return Integer.parseInt(id);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @Builder
    public static class DeviceContainer {
        private String id;
        public Integer getId() {
            return Integer.parseInt(id);
        }
        private String name;
        private LocationContainer location;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @Builder
    public static class Bandwidth {
        private Integer speed;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @Builder
    public static class LocationContainer {
        private String id;
        public Integer getId() {
            return Integer.parseInt(id);
        }
        private String shortName;
        private String latitude;
        private String longitude;
    }

}

