<#-- @ftlvariable name="mxLsps" type="java.util.List<net.es.oscars.pss.params.mx.MxLsp>" -->
<#-- @ftlvariable name="vpls" type="net.es.oscars.pss.params.mx.MxVpls" -->
@version: 1.0.45

delete policy-options community "${vpls.communityName}"
delete policy-options policy-statement "${vpls.policyName}"
delete routing-options forwarding-table export "${vpls.policyName}"
delete routing-instances "${vpls.serviceName}"



