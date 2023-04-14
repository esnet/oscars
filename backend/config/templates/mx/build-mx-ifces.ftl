<#-- @ftlvariable name="ifces" type="java.util.List<net.es.oscars.pss.params.mx.TaggedIfce>" -->
<#-- @ftlvariable name="vpls" type="net.es.oscars.pss.params.mx.MxVpls" -->
@version: 1.0.45

<#list ifces as ifce>
edit interfaces ${ifce.port}
edit unit ${ifce.vlan}
set description "${ifce.description}"
set encapsulation vlan-vpls
set vlan-id ${ifce.vlan}
<#if ifce.vlanSwap??>
set output-vlan-map swap
</#if>
set family vpls filter input "${vpls.statsFilter}"
set family vpls filter output "${vpls.statsFilter}"
top
</#list>
