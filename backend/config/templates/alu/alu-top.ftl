<#-- @ftlvariable name="fragments" type="java.util.List<java.lang.String>" -->
@version: 1.0.45

<#list fragments as fragment>
exit all
${fragment}
exit all
</#list>

