@NsoProxySteps
Feature: Make OSCARS OP Commands netconf ned ready

  I want to verify that OSCARS stats work regardless if the device is on ALU CLI or Nokia NC NED.

  Scenario: Read the NSO Proxy live-status endpoint (happy path)
    Given I have initialized the world
    Given I instantiate the proxy
    When The getLiveStatusShow method is called with device "loc1-cr6" and arguments "service fdb-info"
    Then I did not receive an exception
    Then The resulting esnet-status response is not empty

    When I request to get macs for device "loc1-cr6" and service id 7115
    Then I did not receive an exception
    Then The resulting esnet-status response is not empty
    Then The resulting MAC report status is true

    When I request to get SDPs for device "loc1-cr6" and service id 7115
    Then I did not receive an exception
    Then The resulting esnet-status response is not empty

    When I request to get SAPs for device "loc1-cr6" and service id 7115
    Then I did not receive an exception
    Then The resulting esnet-status response is not empty

    When I request to get LSPs for device "loc1-cr6"
    Then I did not receive an exception
    Then The resulting esnet-status response is not empty
