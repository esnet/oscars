@ModifyControllerSteps
@ModifyControllerStepsHappy
Feature: Test the ModifyController endpoints (Happy)

  Scenario: Modify a connection description at endpoint "/protected/modify/description"
    Given The client executes POST with a new description payload on ModifyController path "/protected/modify/description"
    When The client receives a response from ModifyController
    Then The client receives a ModifyController response status code of 200
    And The ModifyController response is a valid ModifyResponse object

  Scenario: Check to see if the requested schedule is valid at endpoint "/api/valid/schedule"
    Given The client executes POST with a ScheduleRangeRequest payload on ModifyController path "/api/valid/schedule"
    When The client receives a response from ModifyController
    Then The client receives a ModifyController response status code of 200
    And The ModifyController response is a valid ScheduleRangeResponse object

  Scenario: Modify the schedule at endpoint "/protected/modify/schedule"
    Given The client executes POST with a ScheduleRangeRequest payload on ModifyController path "/protected/modify/schedule"
    When The client receives a response from ModifyController
    Then The client receives a ModifyController response status code of 200
    And The ModifyController response is a valid ModifyResponse object

  Scenario: Modify the bandwidth at endpoint "/protected/modify/bandwidth"

  Scenario: Check to see if the requested bandwidth is valid at endpoint "/api/valid/bandwidth"
