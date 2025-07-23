package net.es.oscars.sb.beans;

import lombok.Getter;


@Getter
public enum SyncFeature {
    DISABLED("disabled"), DRY_RUN_ONLY("dry-run-only"), SYNC("sync");
    private final String key;

    SyncFeature(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return getKey();
    }
}
