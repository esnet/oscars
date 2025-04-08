package net.es.oscars.sb.nso.dto;

import lombok.Getter;
import lombok.Setter;
import net.es.oscars.sb.nso.NsoStateSyncer;

@Getter
@Setter
public class NsoStateWrapper<T> {
    public NsoStateSyncer.State state;
    public String description;
    public T instance;
    public NsoStateWrapper(NsoStateSyncer.State state, T localInstance) {
        this.state = state;
        this.instance = localInstance;
        this.description = "";
    }

    public NsoStateWrapper(NsoStateSyncer.State state, T localInstance, String description) {
        this.state = state;
        this.instance = localInstance;
        this.description = description;
    }
}
