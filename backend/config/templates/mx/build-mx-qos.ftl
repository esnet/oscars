<#-- @ftlvariable name="qoses" type="java.util.List<net.es.oscars.pss.params.mx.MxQos>" -->
@version: 1.0.45

<#list qoses as qos>
edit firewall family any filter "${qos.filterName}" term oscars then
set count oscars
set accept
<#if qos.forwarding == "EXPEDITED">
set forwarding-class expedited-forwarding-vc
set loss-priority low
<#else>
set forwarding-class best-effort-vc
set loss-priority high
</#if>
top
<#if qos.createPolicer>
edit firewall policer "${qos.policerName}"
<#if qos.mbps gt 0>
    <#assign bw_limit = qos.mbps+"000000" >
    <#assign burst_limit = qos.mbps+"00000" >
set if-exceeding bandwidth-limit ${bw_limit}
set if-exceeding burst-size-limit ${burst_limit}

    <#if qos.policing == "SOFT">
set then loss-priority high
set then forwarding-class best-effort-vc
    <#else>
set then discard
    </#if>

<#-- if bandwidth == 0 then just mark as scavenger-->
<#else>
set if-exceeding bandwidth-limit 8000
set if-exceeding burst-size-limit 1500
set then loss-priority high
</#if>
top
set firewall family any filter "${qos.filterName}" term oscars then policer "${qos.policerName}"
</#if>

</#list>
