<#-- @ftlvariable name="qosList" type="java.util.List" -->
<#-- @ftlvariable name="qos" type="net.es.oscars.pss.params.alu.AluQos" -->
<#-- @ftlvariable name="protect" type="boolean" -->
<#-- @ftlvariable name="apply" type="boolean" -->
@version: 1.0.45

<#list qosList as qos>

<#assign qosId = qos.policyId >
<#assign sapType = "sap-egress" >
<#if qos.type == "SAP_INGRESS">
    <#assign sapType = "sap-ingress" >
</#if>

/show qos configure qos ${sapType} ${qosId} detail | match "- SAP"
<#--
 - SAP : 3/1/1:3101
 - SAP : 4/1/1:3101
-->


</#list>
