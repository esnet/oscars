<#-- @ftlvariable name="qosList" type="java.util.List" -->
<#-- @ftlvariable name="qos" type="net.es.oscars.pss.params.alu.AluQos" -->
@version: 1.0.45

<#list qosList as qos>
<#assign qosId = qos.policyId >
<#assign sapType = "sap-egress" >
<#if qos.type == "SAP_INGRESS">
    <#assign sapType = "sap-ingress" >
</#if>
/configure qos no ${sapType} ${qosId}
</#list>

