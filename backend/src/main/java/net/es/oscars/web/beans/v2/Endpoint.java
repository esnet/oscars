package net.es.oscars.web.beans.v2;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Endpoint {
    private String connectionId;

    @Schema(description = "The device name", example = "star-cr6")
    public String device;
    @Schema(description = "The port name", example = "1/1/c32/1")
    public String port;
    @Schema(description = "The VLAN id for tagged endpoints", minimum = "0", maximum = "4095", example = "122")
    public int vlan;
    public boolean tagged;
}
