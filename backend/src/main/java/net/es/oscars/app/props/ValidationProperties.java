package net.es.oscars.app.props;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties(ValidationProperties.class)
@ConfigurationProperties(prefix = "validation")
@NoArgsConstructor
public class ValidationProperties {
    @NonNull
    private String projectEsdbOrgName = "Project";

    @NonNull
    private ProjectIdValidationMode projectIdMode = ProjectIdValidationMode.OPTIONAL;

    @Getter
    public enum ProjectIdValidationMode {
        OPTIONAL("optional"),
        MANDATORY("mandatory"),
        PROJECT_IN_ESDB("in-esdb"),
        PROJECT_HAS_USER_WITH_ORC_ID("user-with-orcid"),;
        private final String key;

        ProjectIdValidationMode(String key) {
            this.key = key;
        }
        public static ProjectIdValidationMode fromString(String text) {
            for (ProjectIdValidationMode pivm : ProjectIdValidationMode.values()) {
                if (pivm.key.equalsIgnoreCase(text)) {
                    return pivm;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return getKey();
        }
    }


}
