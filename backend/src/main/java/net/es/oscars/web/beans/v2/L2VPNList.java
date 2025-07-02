package net.es.oscars.web.beans.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.es.oscars.model.L2VPN;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class L2VPNList {
    private int page;
    private int sizePerPage;
    private int totalSize;
    private List<L2VPN> l2vpns;
}
