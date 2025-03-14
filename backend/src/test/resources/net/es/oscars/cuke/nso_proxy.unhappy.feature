@NsoProxySteps
Feature: Make OSCARS OP Commands netconf ned ready

  I want to verify that OSCARS stats work regardless if the device is on ALU CLI or Nokia NC NED.

  Scenario: Read the NSO Proxy live-status endpoint (unhappy path)
    Given I have initialized the world
    # non-existent device, with valid arguments
    When The getLiveStatusShow method is called with device "does-not-exist-cr123" and arguments "service fdb-info"
    Then The resulting esnet-status response contains "error"
    Then The resulting esnet-status response contains "illegal reference"

  Scenario: Read the NSO Proxy live-status endpoint (unhappy path, valid device ID, invalid service ID)
    # valid device ID, non-existent service ID 1111
    When I get SDPs for device "loc1-cr6" and service id 1111
    Then The resulting esnet-status response is empty
    Then The resulting SDP report contains 0 sdps