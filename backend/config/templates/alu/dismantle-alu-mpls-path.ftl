<#-- @ftlvariable name="paths" type="java.util.List" -->
<#-- @ftlvariable name="path" type="net.es.oscars.pss.params.MplsPath" -->
<#-- @ftlvariable name="protect" type="java.lang.Boolean" -->
@version: 1.0.45

<#list paths as path>
/configure router mpls path "${path.name}" shutdown
/configure router mpls no path "${path.name}"
</#list>
