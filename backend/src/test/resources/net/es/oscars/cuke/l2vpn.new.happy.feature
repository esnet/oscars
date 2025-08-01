@L2vpnSteps
@L2vpnStepsHappy
Feature: Submit new L2VPNs successfully

  I want to verify I can submit L2VPNs successfully

  Scenario: Submit a single router L2VPN
    Given I have initialized the world
    Given I have loaded the "l2vpn/one-router.json" L2VPN request
    When I validate the new L2VPN request
    Then the L2VPN request validates successfully
    When I submit the L2VPN request
    Then the L2VPN request did not throw an exception

  Scenario: Submit a two router L2VPN
    Given I have initialized the world
    Given I have loaded the "l2vpn/two-routers.json" L2VPN request
    When I validate the new L2VPN request
    Then the L2VPN request validates successfully
    When I submit the L2VPN request
    Then the L2VPN request did not throw an exception
