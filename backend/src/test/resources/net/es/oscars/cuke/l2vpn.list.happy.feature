@L2vpnSteps
@L2vpnListSteps
@L2vpnListStepsHappy
Feature: List L2VPNs successfully

  I want to verify I can list L2VPNs successfully

  Scenario: List L2VPNs
    Given I have initialized the world
    Given I have cleared all L2VPNs
    Given I have loaded the "l2vpn/one-router.json" L2VPN request
    When I submit the L2VPN request
    Then the L2VPN request did not throw an exception
    Given I have loaded the "l2vpn/two-routers.json" L2VPN request
    When I submit the L2VPN request
    Then the L2VPN request did not throw an exception
    When I list all L2VPNs
    Then the L2VPN list did not throw an exception
    Then the L2VPN list size is 2
    Then the L2VPN list contains "ZKJH"
    Then the L2VPN list contains "XBYZ"


