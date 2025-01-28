package net.es.oscars.pce.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipeSpecification {
    protected String id;

    protected String a;
    protected String z;

    protected Constraints constraints;
    protected List<PipeLSP> lsps;

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Constraints {
        protected List<String> include;
        protected Set<String> exclude;
        @Builder.Default
        protected Protection protection = Protection.LOOSE;
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipeLSP {
        protected List<LspHop> path;
        protected Priority priority;
        protected Role role;
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LspHop {
        protected String urn;
        protected String type;
    }


    public enum Protection {
        LOOSE, NONE, DIVERSE
    }

    public enum Priority {
        PRIMARY, SECONDARY, TERTIARY
    }

    public enum Role {
        WORKING, PROTECT
    }

}
