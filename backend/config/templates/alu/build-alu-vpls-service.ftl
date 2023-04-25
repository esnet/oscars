<#-- @ftlvariable name="vpls" type="net.es.oscars.pss.params.alu.AluVpls" -->
<#-- @ftlvariable name="sap" type="net.es.oscars.pss.params.alu.AluSap" -->
<#-- @ftlvariable name="sdpToVcId" type="net.es.oscars.pss.params.alu.AluSdpToVcId" -->
@version: 1.0.45

<#assign svcId = vpls.svcId>

/configure service vpls ${svcId} name "${vpls.serviceName}" customer 1 create
exit
/configure service vpls ${svcId} shutdown
/configure service vpls ${svcId} description "${vpls.description}"
/configure service vpls ${svcId} service-mtu ${vpls.mtu}
/configure service vpls ${svcId} fdb-table-size 4096
/configure service vpls ${svcId} stp shutdown

<#list vpls.saps as sap>
<#assign sapId = sap.port+":"+sap.vlan>
/configure service vpls ${svcId} sap ${sapId} create
exit
/configure service vpls ${svcId} sap ${sapId} auto-learn-mac-protect
/configure service vpls ${svcId} sap ${sapId} restrict-protected-src discard-frame
/configure service vpls ${svcId} sap ${sapId} description "${sap.description}"
/configure service vpls ${svcId} sap ${sapId} ingress qos ${sap.ingressQosId}
/configure service vpls ${svcId} sap ${sapId} egress qos ${sap.egressQosId}
/configure service vpls ${svcId} sap ${sapId} no shutdown
</#list>

/configure service vpls ${svcId} split-horizon-group "shg" create
/configure service vpls ${svcId} split-horizon-group "shg" auto-learn-mac-protect


<#if vpls.sdpToVcIds??>
<#list vpls.sdpToVcIds as sdpToVcId>
<#assign sdpId = sdpToVcId.sdpId>
<#assign vcId = sdpToVcId.vcId>
<#assign endpointName = sdpToVcId.endpointName>
/configure service vpls ${svcId} endpoint "${endpointName}" create
exit all
/configure service vpls ${svcId} endpoint "${endpointName}" revert-time 60

 <#if vpls.suppressStandby>
/configure service vpls ${svcId} endpoint "${endpointName}" no suppress-standby-signaling
</#if>

/configure service vpls ${svcId} spoke-sdp ${sdpId}:${vcId} vc-type vlan split-horizon-group "shg" endpoint "${endpointName}" create

exit all
/configure service vpls ${svcId} spoke-sdp ${sdpId}:${vcId} stp shutdown

<#if sdpToVcId.primary>
/configure service vpls ${svcId} spoke-sdp ${sdpId}:${vcId} precedence primary
<#else>
/configure service vpls ${svcId} spoke-sdp ${sdpId}:${vcId} no pw-status-signaling
</#if>


<#if sdpToVcId.besteffort>
/configure service vpls ${svcId} spoke-sdp ${sdpId}:${vcId} egress qos 3 port-redirect-group "best-effort-vc" instance 1
</#if>
/configure service vpls ${svcId} spoke-sdp ${sdpId}:${vcId} no shutdown
</#list>
</#if>

/configure service vpls ${svcId} no shutdown
