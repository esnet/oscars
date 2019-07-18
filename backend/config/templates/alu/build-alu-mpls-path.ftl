<#-- @ftlvariable name="paths" type="java.util.List" -->
<#-- @ftlvariable name="path" type="net.es.oscars.pss.params.MplsPath" -->
<#-- @ftlvariable name="protect" type="java.lang.Boolean" -->

<#list paths as path>
/configure router mpls path "${path.name}" shutdown
    <#list path.hops as hop>
/configure router mpls path "${path.name}" hop ${hop.order} ${hop.address} strict
    </#list>
/configure router mpls path "${path.name}" no shutdown
</#list>
