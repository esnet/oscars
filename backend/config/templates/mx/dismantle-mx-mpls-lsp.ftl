<#-- @ftlvariable name="lsps" type="java.util.List<net.es.oscars.pss.params.mx.MxLsp>" -->

<#list lsps as mxlsp>
delete protocols mpls label-switched-path "${mxlsp.lsp.name}"
</#list>


