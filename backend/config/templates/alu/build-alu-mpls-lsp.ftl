<#-- @ftlvariable name="lsps" type="java.util.List" -->
<#-- @ftlvariable name="lsp" type="net.es.oscars.pss.params.Lsp" -->
@version: 1.0.45

<#list lsps as lsp>
<#assign lspName = lsp.name >
/configure router mpls lsp "${lspName}" shutdown
/configure router mpls lsp "${lspName}" to ${lsp.to}
/configure router mpls lsp "${lspName}" primary "${lsp.pathName}" priority ${lsp.setupPriority} ${lsp.holdPriority}
/configure router mpls lsp "${lspName}" metric ${lsp.metric}
/configure router mpls lsp "${lspName}" no shutdown
</#list>

