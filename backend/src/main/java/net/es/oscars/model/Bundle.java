package net.es.oscars.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import net.es.oscars.model.enums.Protection;
import net.es.oscars.topo.enums.UrnType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Jacksonized
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

@Entity
@Table
public class Bundle {
    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "l2vpn_id")
    @JsonIgnore
    private L2VPN l2vpn;

    protected String name;
    protected String a;
    protected String z;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    protected Protection protection = Protection.LOOSE;

    @Embedded
    protected Constraints constraints;

    @OneToMany
    @Builder.Default
    protected List<LSP> lsps = new ArrayList<>();

    @Jacksonized
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Constraints {

        @ElementCollection
        @CollectionTable(name="include", joinColumns=@JoinColumn(name="bundle_id"))
        @Column(name="include")
        protected List<Waypoint> include;

        @ElementCollection
        @CollectionTable(name="exclude", joinColumns=@JoinColumn(name="bundle_id"))
        @Column(name="exclude")
        protected Set<Waypoint> exclude;

        public List<String> includedUrns() {
            return include.stream().map(Waypoint::getUrn).collect(Collectors.toList());
        }
        public Set<String> excludedUrns() {
            return include.stream().map(Waypoint::getUrn).collect(Collectors.toSet());
        }


    }

    @Jacksonized
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Waypoint {
        protected String urn;
        protected UrnType type;
    }


}
