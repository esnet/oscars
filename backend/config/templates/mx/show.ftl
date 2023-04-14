<#-- @ftlvariable name="vpls" type="net.es.oscars.pss.params.mx.MxVpls" -->
<#-- @ftlvariable name="ifces" type="java.util.List<net.es.oscars.pss.params.mx.TaggedIfce>" -->
<#-- @ftlvariable name="lsps" type="java.util.List<net.es.oscars.pss.params.mx.MxLsp>" -->
@version: 1.0.45

show vpls connection instance "${vpls.serviceName}" extensive
show vpls mac-table instance "${vpls.serviceName}"

<#list lsps as mxlsp>
show mpls lsp name "${mxlsp.lsp.name}" detail
</#list>

<#list ifces as ifce>
show interfaces ${ifce.port}.${ifce.vlan} detail
</#list>

<#list ifces as ifce>
show firewall filter "${vpls.statsFilter}-${ifce.port}.${ifce.vlan}-i" detail
show firewall filter "${vpls.statsFilter}-${ifce.port}.${ifce.vlan}-o" detail
</#list>

<#list lsps as mxlsp>
    <#if mxlsp.policeFilter??>
show firewall filter "${mxlsp.policeFilter}-${mxlsp.lsp.name}"
    </#if>
</#list>