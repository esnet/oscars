package net.es.oscars.pce.beans;

import lombok.*;

@Builder
@Data
@EqualsAndHashCode
public class YenkVertex {

    private Type type;

    private String urn;

    public enum Type {
        DEVICE,
        PORT
    }


}
