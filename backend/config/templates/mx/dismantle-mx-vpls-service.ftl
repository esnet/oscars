<#-- @ftlvariable name="vpls" type="net.es.oscars.pss.params.mx.MxVpls" -->
@version: 1.0.45

<#if vpls.loopback??>
edit interfaces lo0 unit 0 family inet
delete address ${vpls.loopback}
</#if>
top
<#if vpls.statsFilter??>
delete firewall family vpls filter "${vpls.statsFilter}"
</#if>

