package net.es.oscars.resv.svc.validators;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.es.oscars.app.props.ValidationProperties;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbOrganization;
import net.es.oscars.dto.esdb.gql.GraphqlEsdbOrganizationType;
import net.es.oscars.esdb.ESDBProxy;
import net.es.oscars.resv.enums.ConnectionMode;
import net.es.oscars.web.simple.SimpleConnection;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
public class ConnServiceProjectIdValidate implements Validator, ValidatorWithErrors {
    private ValidationProperties props;
    private ESDBProxy esdbProxy;

    Map<String, Errors> allErrors = new HashMap<>();

    public ConnServiceProjectIdValidate(ValidationProperties properties, ESDBProxy esdbProxy) {
        this.props = properties;
        this.esdbProxy = esdbProxy;
    }

    /**
     * Check if this validator supports the class provided as an argument.
     * @param clazz The class to check.
     * @return Boolean. True if supported. This class supports SimpleConnection class.
     */
    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return SimpleConnection.class.isAssignableFrom(clazz);
    }

    /**
     * Validate the target object, and return errors by reference, if any.
     * Will set the checkedBeginTime and checkedEndTime Instant values.
     * @param target The target SimpleConnection object to validate.
     * @param errors The list of errors, if any.
     */
    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {

        SimpleConnection inConn = (SimpleConnection) target;
        switch (props.getProjectIdMode()) {
            case OPTIONAL -> {
                // no validation on the projectId field
                return;
            }
            case MANDATORY -> {
                verifyFieldPresent(inConn, errors);
            }
            case PROJECT_IN_ESDB -> {
                verifyFieldPresent(inConn, errors);
                verifyProjectId(inConn, errors, false);

            }
            case PROJECT_HAS_USER_WITH_ORC_ID -> {
                verifyFieldPresent(inConn, errors);
                verifyProjectId(inConn, errors, true);

            }
        }
    }

    public void verifyProjectId(SimpleConnection inConn, Errors errors, boolean mustHaveUserWithOrcId) {
        String orgTypeUuid = getOrgTypeUiid(props.getProjectEsdbOrgName(), esdbProxy);

        if (orgTypeUuid == null) {
            errors.rejectValue("projectIds", "INTERNAL_ERROR", "Unable to get org type uuid from ESDB for 'Project'");
        } else {
            // get the project orgs from ESDB
            List<GraphqlEsdbOrganization> esdbOrgs = getEsdbProjectOrgs(esdbProxy, orgTypeUuid, mustHaveUserWithOrcId);
            if (inConn.getProjectIds() == null) {
                // return instead of NPEing; verifyFieldPresent() will set the error
                return;
            }

            // now, check that each project ids is contained in the list returned by ESDB
            for (String projectId : inConn.getProjectIds()) {
                boolean found = false;
                for (GraphqlEsdbOrganization esdbOrg : esdbOrgs) {
                    if (esdbOrg.getUuid().equals(projectId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    String orcIdMsg = "";
                    if (mustHaveUserWithOrcId) {
                        orcIdMsg = " (project MUST have a user with a valid orcid)";
                    }
                    errors.rejectValue("projectIds", "INVALID_PROJECT_ID", "Project id "+projectId+" not found in ESDB."+orcIdMsg);
                }
            }
        }

        if (errors.hasErrors()) {
            allErrors.put("projectIds", errors);
        }
    }

    /**
     *
     * @param esdbProxy the esdb proxy
     * @param orgTypeUuid the UUID of the org-type to filter on
     * @param mustHaveUserWithOrcId whether we require projects to have a user with a project id
     * @return the list of ESDB orgs that match
     */

    public static List<GraphqlEsdbOrganization> getEsdbProjectOrgs(ESDBProxy esdbProxy, String orgTypeUuid, boolean mustHaveUserWithOrcId) {
        // fetch list of orgs from ESDB, filtered by the uuid of the org-type we want
        List<GraphqlEsdbOrganization> esdbOrgs = esdbProxy.gqlOrganizationList(orgTypeUuid);

        // if the project org MUST have a user with a valid orc-id, filter out orgs
        // that do not have at least one of those.
        if (mustHaveUserWithOrcId) {
            List<GraphqlEsdbOrganization> filteredOrgs = new ArrayList<>();
            for (GraphqlEsdbOrganization esdbOrg : esdbOrgs) {
                boolean hasUserWithOrcId = false;
                for (GraphqlEsdbOrganization.GraphEsdbContact contact : esdbOrg.getContacts()) {
                    String orcId = contact.getContact().getOrcid();
                    if (validateOrcId(orcId)) {
                        hasUserWithOrcId = true;
                    }
                }
                if (hasUserWithOrcId) {
                    filteredOrgs.add(esdbOrg);
                }
            }
            esdbOrgs = filteredOrgs;
        }
        return esdbOrgs;
    }


    /**
     * gets the UUID of a named orgType from ESDB
     *
     * @param orgTypeName the org-type name
     * @param esdbProxy the ESDB proxy object
     * @return the UUID that matches the org-type, or null if not found
     */
    public static String getOrgTypeUiid(String orgTypeName, ESDBProxy esdbProxy) {
        List<GraphqlEsdbOrganizationType> esdbOrganizationTypeList = esdbProxy.gqlOrganizationTypeList();
        for (GraphqlEsdbOrganizationType type : esdbOrganizationTypeList) {
            if (type.getName().equals(orgTypeName)) {
                return type.getUuid();
            }
        }
        return null;
    }

    /**
     * see https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier
     *
     * @param orcId the orcid to validate
     * @return whether the orcid is valid
     */
    public static boolean validateOrcId(String orcId) {
        if (orcId == null) {
            return false;
        }
        // format is http://orcid.org/0000-0002-1825-0097
        String regex = "^https://orcid.org/\\d{4}-\\d{4}-\\d{4}-\\d{3}[0-9X]{1}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(orcId);
        return matcher.find();
    }

    /**
     * verify that the projectIds field is non-null, AND has at least one non-empty value
     *
     * @param inConn the connection to validate
     * @param errors the errors object
     */
    private void verifyFieldPresent(SimpleConnection inConn, Errors errors) {
        // at least one value in the projectIds field
        if (inConn.getProjectIds() == null || inConn.getProjectIds().isEmpty()) {
            errors.rejectValue("projectIds", "NO_PROJECT_IDS", "No project IDs provided");
        } else {
            boolean allEmpty = true;
            for (String projectId : inConn.getProjectIds()) {
                if (projectId != null && !projectId.isEmpty()) {
                    allEmpty = false;
                }
            }
            if (allEmpty) {
                errors.rejectValue("projectIds", "NO_PROJECT_IDS", "Only empty project IDs provided");
            }
        }
    }



    public boolean hasErrors() {
        return !allErrors.isEmpty();
    }
}

