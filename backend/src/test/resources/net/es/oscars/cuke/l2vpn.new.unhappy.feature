@L2vpnSteps
@L2vpnStepsUnhappy
Feature: Submit new L2VPNs unsuccessfully

  I want to verify I can validate L2VPNs and reject invalid ones

  Scenario: Submit a single router L2VPN
    Given I have initialized the world
    Given I have loaded the "l2vpn/one-router-too-much-bw.json" L2VPN request
    When I validate the new L2VPN request
    Then the L2VPN request does not validate successfully
    When I submit the L2VPN request
    Then the L2VPN request did throw an exception

  Scenario: Submit a two router L2VPN
    Given I have initialized the world
    Given I have loaded the "l2vpn/two-routers-too-much-bw.json" L2VPN request
    When I validate the new L2VPN request
    Then the L2VPN request does not validate successfully
    When I submit the L2VPN request
    Then the L2VPN request did throw an exception
