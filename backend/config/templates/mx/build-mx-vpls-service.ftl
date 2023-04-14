<#-- @ftlvariable name="vpls" type="net.es.oscars.pss.params.mx.MxVpls" -->
<#-- @ftlvariable name="ifces" type="java.util.List<net.es.oscars.pss.params.mx.TaggedIfce>" -->
@version: 1.0.45

edit routing-instances "${vpls.serviceName}"
<#list ifces as ifce>
set interface ${ifce.port}.${ifce.vlan}
</#list>
set instance-type vpls
edit protocols vpls
set no-tunnel-services
set mtu ${vpls.mtu}
edit site CE
<#list ifces as ifce>
set interface ${ifce.port}.${ifce.vlan} mac-pinning
</#list>

<#if vpls.loopback??>
top
edit interfaces lo0 unit 0 family inet
set address ${vpls.loopback}
</#if>

<#if vpls.statsFilter??>
top
edit firewall family vpls filter "${vpls.statsFilter}"
set interface-specific
set term oscars then count oscars_counter
set term oscars then accept
</#if>
