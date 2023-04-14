<#-- @ftlvariable name="lsps" type="java.util.List<net.es.oscars.pss.params.mx.MxLsp>" -->
@version: 1.0.45

<#list lsps as mxlsp>
top
edit protocols mpls label-switched-path "${mxlsp.lsp.name}"
set to ${mxlsp.lsp.to}
set metric ${mxlsp.lsp.metric}
set no-cspf
set priority ${mxlsp.lsp.setupPriority} ${mxlsp.lsp.holdPriority}
set primary "${mxlsp.lsp.pathName}"
<#if mxlsp.policeFilter??>
set policing filter "${mxlsp.policeFilter}"
</#if>
</#list>