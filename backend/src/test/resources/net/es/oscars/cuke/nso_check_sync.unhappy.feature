@NsoCheckSyncSteps
Feature: Run NSO check-sync

  I want to verify that device check-sync works

  Scenario: Empty body
    Given I have initialized the world
    Given I mock the check-sync to return an empty body
    When The check-sync method is called
    Then I did not receive an exception
    Then The resulting check-sync response result is "unknown"

  Scenario: Null output
    Given I mock the check-sync to return null output
    When The check-sync method is called
    Then I did not receive an exception
    Then The resulting check-sync response result is "unknown"

  Scenario: Null result
    Given I mock the check-sync to return null result
    When The check-sync method is called
    Then I did not receive an exception
    Then The resulting check-sync response result is "unknown"

  Scenario: NSO error
    Given I mock the check-sync to return an NSO error
    When The check-sync method is called
    Then I did not receive an exception
    Then The resulting check-sync response result is "error"

  Scenario: REST error
    Given I mock the check-sync to return a REST error
    When The check-sync method is called
    Then I did not receive an exception
    Then The resulting check-sync response result is "unknown"
