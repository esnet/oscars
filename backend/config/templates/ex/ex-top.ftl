<#-- @ftlvariable name="fragments" type="java.util.List<java.lang.String>" -->
@version: 1.0.45

configure private

<#list fragments as fragment>
top
${fragment}
top
</#list>

commit and-quit