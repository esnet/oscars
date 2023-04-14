<#-- @ftlvariable name="qos" type="net.es.oscars.pss.params.alu.AluQos" -->
<#-- @ftlvariable name="qosList" type="java.util.List" -->
@version: 1.0.45

<#list qosList as qos>

<#if qos.mbps gt 0>
    <#assign bps = qos.mbps+"000" >
<#else>
    <#assign bps = "0" >
</#if>

<#assign max = bps >
<#if qos.policing == "SOFT" >
    <#assign max = "max" >
</#if>
<#assign sapType = "sap-egress" >
<#if qos.type == "SAP_INGRESS">
    <#assign sapType = "sap-ingress" >
</#if>

/configure qos ${sapType} ${qos.policyId} queue 2 rate ${max} cir ${bps}
</#list>