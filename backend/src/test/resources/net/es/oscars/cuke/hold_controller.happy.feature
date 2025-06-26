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
   