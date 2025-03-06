@NsoProxySteps
Feature: Make OSCARS OP Commands netconf ned ready

  I want to verify that OSCARS stats work regardless if the device is on ALU CLI or Nokia NC NED.

  Scenario: Read the NSO Proxy live-status endpoint (happy path)
    Given I have initialized the world
    When The getLiveStatusShow method is called with device "loc1-cr6" and arguments "service fdb-info"
    Then I did not receive an exception
    Then The resulting esnet-status response is not empty
    Then The resulting esnet-status response is not an error message

    When I get macs for device "loc1-cr6" and service id 7093
    Then I did not receive an exception
    Then The resulting esnet-status response is not empty
    Then The resulting esnet-status response is not an error message
    Then The resulting MAC report status is true

    When I get SDPs for device "loc1-cr6" and service id 7115
    Then I did not receive an exception
    Then The resulting esnet-status response is not empty
    Then The resulting esnet-status response is not an error message
    Then The resulting SDP report contains 2 sdps

    When I get SAPs for device "loc1-cr6" and service id 7115
    Then I did not receive an exception
    Then The resulting esnet-status response is not empty
    Then The resulting esnet-status response is not an error message
    Then The resulting SAP report contains 1 saps

    When I get LSPs for device "loc1-cr6"
    Then I did not receive an exception
    Then The resulting esnet-status response is not empty
    Then The resulting esnet-status response is not an error message
    Then The resulting LSP report contains 2 lsps