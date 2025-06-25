@ConnControllerSteps
@ConnControllerStepsHappy

Feature: ConnController HTTP API endpoints.
  Scenario: Generate a connection ID.
    Given The client executes "GET" on ConnController path "/protected/conn/generateId"
    When The client receives a response from ConnController
    Then The client receives a ConnController response status code of 200
    And The client receives a ConnController response payload
    And The ConnController generated ID is valid