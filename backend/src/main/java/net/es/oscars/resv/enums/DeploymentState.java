package net.es.oscars.resv.enums;

public enum DeploymentState {

    UNDEPLOYED,
    WAITING_TO_BE_DEPLOYED,
    BEING_DEPLOYED,

    DEPLOYED,
    WAITING_TO_BE_UNDEPLOYED,
    BEING_UNDEPLOYED,

    DEPLOY_FAILED,

    UNDEPLOY_FAILED,
}
