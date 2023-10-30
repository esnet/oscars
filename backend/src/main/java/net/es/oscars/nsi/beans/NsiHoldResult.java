package net.es.oscars.nsi.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.nsi.lib.soap.gen.nsi_2_0.framework.types.TypeValuePairType;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NsiHoldResult {
    private Boolean success;
    private String errorMessage;
    private NsiErrors errorCode;
    private List<TypeValuePairType> tvps;
}
