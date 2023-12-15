package net.es.oscars.sb.nso.ent;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NsoVirtInterface {

    @Id
    @GeneratedValue
    private Long id;

    private String connectionId;

    private String device;

    private List<String> ipAddresses;


}
