@EseApiControllerSteps
@EseApiControllerStepsHappy
Feature: EseApiController Endpoints for l2VPNs
  Scenario: Create a new L2VPN
    Given I have loaded the "l2vpn/two-routers.json" L2VPN request
    Given The client executes POST with a L2VPN payload on EseApiController path "/protected/l2vpn/new"
    When The client receives a response from EseApiController
    Then The client receives a EseApiController response status code of 200
    And The EseApiController response is a valid L2VPN object
    And The EseApiController response L2VPN object's meta username property matches "anonymous"

  Scenario: Get the l2VPN of connection ID "XBYZ"
    Given The client executes "GET" on EseApiController path "/api/l2vpn/get/XBYZ"
    When The client receives a response from EseApiController
    Then The client receives a EseApiController response status code of 200
    And The EseApiController response is a valid L2VPN object

  Scenario: List l2VPNs
    Given The client executes POST with a ConnectionFilter payload on EseApiController path "/api/l2vpn/list"
    When The client receives a response from EseApiController
    Then The client receives a EseApiController response status code of 200
    And The EseApiController response is a valid L2VPNList object

  Scenario: Validate new L2VPN (Not implemented yet)
    Given The client executes POST with a L2VPN payload on EseApiController path "/api/l2vpn/validate"
    When The client receives a response from EseApiController
    Then The client receives a EseApiController response status code of 200
    And The EseApiController response is a valid ValidationResponse object

#  Scenario: Validate existing L2VPN (Not implemented yet)
#    Given The client executes PUT with a L2VPN payload on EseApiController path "/api/l2vpn/validate"
#    When The client receives a response from EseApiController
#    Then The client receives a EseApiController response status code of 200
#    And The EseApiController response is a valid ValidationResponse object


#  Scenario: Replace an L2VPN (Not implemented yet)
#    Given The client executes PUT with a L2VPN payload on EseApiController path "/api/l2vpn/replace"
#    When The client receives a response from EseApiController
#    Then The client receives a EseApiController response status code of 200
#    And The EseApiController response is a valid L2VPN object

  Scenario: Check availability of an L2VPN
    Given The client executes POST with a L2VPN payload on EseApiController path "/api/l2vpn/availability"
    When The client receives a response from EseApiController
    Then The client receives a EseApiController response status code of 200
    And The EseApiController response is a valid BandwidthAvailabilityResponse object