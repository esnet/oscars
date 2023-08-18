package net.es.oscars.web.simple;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.web.beans.PceMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pipe {
    protected String a;
    protected String z;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Integer mbps;

    protected Integer azMbps;
    protected Integer zaMbps;
    protected Boolean protect;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected PceMode pceMode;

    @Builder.Default
    protected List<String> ero = new ArrayList<>();

    @Builder.Default
    protected List<String> exclude = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Validity validity;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder.Default
    protected Map<String, Validity> eroValidity = new HashMap<>();

}
