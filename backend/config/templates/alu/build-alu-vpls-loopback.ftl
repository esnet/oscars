<#-- @ftlvariable name="loopback_ifce_name" type="java.lang.String" -->
<#-- @ftlvariable name="loopback_address" type="java.lang.String" -->
@version: 1.0.45

/configure router interface "${loopback_ifce_name}" address ${loopback_address}/32
/configure router interface "${loopback_ifce_name}" loopback
/configure router interface "${loopback_ifce_name}" enable-ingress-stats
/configure router interface "${loopback_ifce_name}" no shutdown
/configure router pim interface "${loopback_ifce_name}" shutdown


