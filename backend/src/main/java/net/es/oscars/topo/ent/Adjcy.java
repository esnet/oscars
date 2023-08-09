package net.es.oscars.topo.ent;

import lombok.*;
import net.es.oscars.topo.enums.Layer;

import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Adjcy {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Point a;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Point z;

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null ) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Adjcy other = (Adjcy) obj;
        return id != null && id.equals(other.getId());
    }


    /**
     *
     * @param other
     * @return true if the adjacencies are between the same points and have the same metrics
     */
    public boolean equivalent(Adjcy other) {
        if (this.equals(other)) {
            return true;
        }
        boolean aisa = this.a.same(other.getA());
        boolean aisz = this.a.same(other.getZ());

        boolean zisa = this.z.same(other.getA());
        boolean zisz = this.z.same(other.getZ());

        boolean mightBeEquivalent = false;

        if (aisa && zisz) {
            mightBeEquivalent = true;
        } else if (aisz && zisa){
            mightBeEquivalent = true;
        }
        if (mightBeEquivalent) {
            return this.metrics.equals(other.getMetrics());
        } else {
            return false;
        }
    }


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable
    @Builder.Default
    private Map<Layer, Long> metrics = new HashMap<>();

    public String getUrn() {
        return a.getUrn() + " - " + z.getUrn();
    }


    public String toString() {
        return this.getClass().getSimpleName() + "-" + this.getUrn() + " " + getId();
    }


}
