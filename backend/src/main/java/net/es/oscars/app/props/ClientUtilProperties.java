package net.es.oscars.app.props;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "client-util")
@Data
@Component
@NoArgsConstructor
public class ClientUtilProperties {
    public boolean enableGzipCompression = false;
    public boolean forceGzip = false;
    public int gzipThreshold = 0;
    // Comma separated content types
    public String gzipContentTypes = "application/xml";

    public List<String> getContentTypes() {
        return Arrays.asList(gzipContentTypes.split(","));
    }
}
