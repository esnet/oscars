<#-- @ftlvariable name="mxLsps" type="java.util.List<net.es.oscars.pss.params.mx.MxLsp>" -->
<#-- @ftlvariable name="vpls" type="net.es.oscars.pss.params.mx.MxVpls" -->
@version: 1.0.45

<#assign communityMembers = "65000:672277L:"+vpls.communityId>

set policy-options community "${vpls.communityName}" members ${communityMembers}

top
edit policy-options policy-statement "${vpls.policyName}" term oscars
set from community "${vpls.communityName}"
<#list mxLsps as mxlsp>
set then install-nexthop lsp "${mxlsp.lsp.name}"
</#list>
set then accept
top

set routing-options forwarding-table export [ "${vpls.policyName}" ]

<#list mxLsps as mxlsp>
    <#assign lsp_neighbor = mxlsp.neighbor>
    <#assign mesh_group = "sdp-wrk-"+vpls.vcId >
    <#assign vplsId = vpls.vcId>
    <#if !mxlsp.primary && vpls.protectEnabled>
        <#assign vplsId = vpls.protectVcId>
        <#assign mesh_group = "sdp-prt-"+vplsId >
    </#if>

edit routing-instances "${vpls.serviceName}" protocols vpls mesh-group "${mesh_group}"
set local-switching
set vpls-id ${vplsId}
edit neighbor ${lsp_neighbor}
set psn-tunnel-endpoint ${mxlsp.lsp.to}
set community "${vpls.communityName}"
set encapsulation-type ethernet-vlan
top
</#list>
