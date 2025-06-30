@EseApiControllerSteps
@EseApiControllerStepsHappy
Feature: EseApiController Endpoints for l2VPNs
  Scenario: Get the l2VPN of connection ID "ABCD"
    Given The client executes "GET" on EseApiController path "/api/l2vpn/get/ABCD"
    When The client receives a response from EseApiController
    Then The client receives a EseApiController response status code of 200
    And The EseApiController response is a valid L2VPN object

  Scenario: List l2VPNs
    Given The client executes POST with a ConnectionFilter payload on EseApiController path "/api/l2vpn/list"
    When The client receives a response from EseApiController
    Then The client receives a EseApiController response status code of 200
    And The EseApiController response is a valid L2VPNList object