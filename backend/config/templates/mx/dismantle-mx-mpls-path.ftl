<#-- @ftlvariable name="paths" type="java.util.List<net.es.oscars.pss.params.MplsPath>" -->
@version: 1.0.45

<#list paths as path>
delete protocols mpls path "${path.name}"
</#list>
