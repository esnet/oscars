@HoldControllerSteps
@HoldControllerStepsHappy
Feature: Test the HoldController endpoints (Happy)
  Scenario: Extend a connection hold
    Given The client executes "GET" on HoldController path "/protected/extend_hold/ABCD"
    When The client receives a response from HoldController
    Then The client receives a HoldController response status code of 200
    And The HoldController response is a valid Instant object

  Scenario: Get a list of currently held entries
    Given The client executes "GET" on HoldController path "/protected/held/current"
    When The client receives a response from HoldController
    Then The client receives a HoldController response status code of 200
    And The HoldController response is a valid list of CurrentlyHeldEntry objects

  Scenario: Clear a hold on a connection
    Given The client executes "GET" on HoldController path "/protected/held/clear/ABCD"
    When The client receives a response from HoldController
    Then The client receives a HoldController response status code of 200
    # No response body expected

  Scenario: Sending an HTTP POST payload to /protected/cloneable
    Given The client executes POST with SimpleConnection payload on HoldController path "/protected/cloneable"
    When The client receives a response from HoldController
    Then The client receives a HoldController response status code of 200
    And The HoldController response is a valid SimpleConnection

  Scenario: Sending an HTTP POST payload to /protected/hold
    # Note, /protected/pcehold just calls the function handler for /protected/hold
    Given The client executes POST with SimpleConnection payload on HoldController path "/protected/hold"
    When The client receives a response from HoldController
    Then The client receives a HoldController response status code of 200
    And The HoldController response is a valid SimpleConnection