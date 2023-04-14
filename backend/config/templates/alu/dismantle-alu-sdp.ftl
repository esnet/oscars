<#-- @ftlvariable name="sdps" type="java.util.List" -->
<#-- @ftlvariable name="sdp" type="net.es.oscars.pss.params.alu.AluSdp" -->
<#-- @ftlvariable name="protect" type="java.lang.Boolean" -->
@version: 1.0.45

<#list sdps as sdp>
<#assign sdpId = sdp.sdpId>

# service distribution point - forwards packets to the MPLS tunnel
/configure service sdp ${sdpId} shutdown
/configure service no sdp ${sdpId}
</#list>

