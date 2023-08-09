package net.es.oscars.pss.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AluTemplatePaths {
    private String sdp;
    private String path;
    private String lsp;
    private String qos;
    private String vpls;
    private String loopback;

}
