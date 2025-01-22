package net.es.oscars.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import net.es.oscars.model.enums.Protection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


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

    @Embeddable
    @Table
    public static class Constraints {

        @ElementCollection
        @CollectionTable(name="include", joinColumns=@JoinColumn(name="bundle_id"))
        @Column(name="include")
        protected List<String> include;

        @ElementCollection
        @CollectionTable(name="exclude", joinColumns=@JoinColumn(name="bundle_id"))
        @Column(name="exclude")
        protected Set<String> exclude;


    }


}
