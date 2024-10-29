package net.es.oscars.nsi.beans;

public enum NsiEvent {
    TERM_CF,

    RESV_START,
    RESV_CF,
    RESV_FL,

    COMMIT_START,
    COMMIT_FL,
    COMMIT_CF,
    FORCED_END,
    PROV_START,
    PROV_CF,
    REL_START,
    REL_CF,
    ABORT_START,
    ABORT_CF,
    RESV_TIMEOUT,
    RESV_CHECK
}
