package net.es.oscars.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
public class Endpoint {
    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "l2vpn_id")
    @JsonIgnore
    private L2VPN l2vpn;


    @Schema(description = "The device name", example = "star-cr6")
    public String device;

    @Schema(description = "The port name", example = "1/1/c32/1")
    public String port;

    @Schema(description = "The VLAN id (for tagged endpoints)", minimum = "0", maximum = "4095", example = "122")
    public int vlan;

    public boolean tagged;
}
