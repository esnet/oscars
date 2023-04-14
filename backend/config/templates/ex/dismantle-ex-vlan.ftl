<#-- @ftlvariable name="vlans" type="java.util.List<net.es.oscars.pss.params.ex.ExVlan>" -->
@version: 1.0.45

<#list vlans as vlan>

<#list vlan.ifces as ifce>
edit interfaces ${ifce.port} unit 0 family ethernet-switching vlan
delete members ${vlan.name}
</#list>

delete vlans vlan ${vlan.name}

</#list>






