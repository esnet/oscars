<#-- @ftlvariable name="paths" type="java.util.List<net.es.oscars.pss.params.MplsPath>" -->
@version: 1.0.45

<#list paths as path>
edit protocols mpls path "${path.name}"
<#list path.hops as hop>
set ${hop.address} strict
</#list>
top
</#list>
