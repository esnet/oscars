<#-- @ftlvariable name="sdps" type="java.util.List" -->
<#-- @ftlvariable name="sdp" type="net.es.oscars.pss.params.alu.AluSdp" -->
<#-- @ftlvariable name="protect" type="java.lang.Boolean" -->
@version: 1.0.45

<#list sdps as sdp>
<#assign sdpId = sdp.sdpId>
/configure service sdp ${sdpId} mpls create
/configure service sdp ${sdpId} shutdown
/configure service sdp ${sdpId} description "${sdp.description}"
/configure service sdp ${sdpId} far-end ${sdp.farEnd}
/configure service sdp ${sdpId} lsp "${sdp.lspName}"
/configure service sdp ${sdpId} keep-alive shutdown
/configure service sdp ${sdpId} no shutdown
exit all
</#list>
