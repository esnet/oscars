<#-- @ftlvariable name="vlans" type="java.util.List<net.es.oscars.pss.params.ex.ExVlan>" -->
@version: 1.0.45

<#list vlans as vlan>

edit vlans ${vlan.name}
<#if vlan.description??>
set description "${vlan.description}"
</#if>
set vlan-id ${vlan.vlanId}
top

<#list vlan.ifces as ifce>
edit interfaces ${ifce.port} unit 0 family ethernet-switching vlan
set members ${vlan.name}
</#list>

</#list>






