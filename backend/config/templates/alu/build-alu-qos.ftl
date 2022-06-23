<#-- @ftlvariable name="qosList" type="java.util.List" -->
<#-- @ftlvariable name="qos" type="net.es.oscars.pss.params.alu.AluQos" -->
<#-- @ftlvariable name="protect" type="boolean" -->
<#-- @ftlvariable name="apply" type="boolean" -->
@version: 1.0.37

<#list qosList as qos>

    <#assign qosId = qos.policyId >
    <#assign sapType = "sap-egress" >
    <#if qos.type == "SAP_INGRESS">
        <#assign sapType = "sap-ingress" >
    </#if>

<#-- pir / cir is expressed in kbps -->
    <#if qos.mbps gt 0>
        <#assign cir = qos.mbps+"000" >
    <#else>
        <#assign cir = "0" >
    </#if>

    <#assign pir = cir >
    <#if qos.policing == "SOFT" >
        <#assign pir = "max" >
    <#else>
    <#-- pir range is 1kbps and upwards, even if cir == 0-->
        <#if pir == "0" >
            <#assign pir = "1" >
        </#if>
    </#if>





<#-- shared for ingress and egress -->
    /configure qos ${sapType} ${qosId} create
    /configure qos ${sapType} ${qosId} description "${qos.description}"
    /configure qos ${sapType} ${qosId} fc "be" create
    /configure qos ${sapType} ${qosId} fc "ef" create
    /configure qos ${sapType} ${qosId} fc "l1" create


<#-- ingress only -->
    <#if qos.type == "SAP_INGRESS">
        /configure qos ${sapType} ${qosId} queue 5 create
        exit
        /configure qos ${sapType} ${qosId} fc "l1" queue 5
        exit

        /configure qos ${sapType} ${qosId} queue 6 create
        exit
        /configure qos ${sapType} ${qosId} fc "ef" queue 6
        /configure qos ${sapType} ${qosId} fc "ef" broadcast-queue 11
        /configure qos ${sapType} ${qosId} fc "ef" unknown-queue 11

        /configure qos ${sapType} ${qosId} queue 11 multipoint create
        /configure qos ${sapType} ${qosId} queue 11 rate 100 cir 100
        exit

    <#-- ingress, only when we apply QoS-->
        <#if apply>
            /configure qos ${sapType} ${qosId} default-fc "ef"
            /configure qos ${sapType} ${qosId} queue 6 rate ${pir} cir ${cir}
        <#else>
            /configure qos ${sapType} ${qosId} default-fc "l1"
            /configure qos ${sapType} ${qosId} queue 5 rate max cir 0
        </#if>


    <#-- egress only -->
    <#else>
        /configure qos ${sapType} ${qosId} queue 1 create
        exit

        /configure qos ${sapType} ${qosId} queue 5 best-effort create
        exit
    <#-- egress only,when protect is set TODO: check w chin -->
        <#if protect??>
            /configure qos ${sapType} ${qosId} queue 5 rate max cir 1000
            exit
        </#if>

        /configure qos ${sapType} ${qosId} queue 6 expedite create
        exit

        /configure qos ${sapType} ${qosId} fc "be" queue 1
        /configure qos ${sapType} ${qosId} fc "l1" queue 5
        /configure qos ${sapType} ${qosId} fc "ef" queue 6


    <#-- egress only, depending on whether we apply QoS -->
        <#if apply>
            /configure qos ${sapType} ${qosId} queue 6 rate ${pir} cir ${cir}
        <#else>
            /configure qos ${sapType} ${qosId} queue 5 rate max cir 0
        </#if>

    </#if>

</#list>