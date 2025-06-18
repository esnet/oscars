@NsoCheckSyncSteps
Feature: Run NSO check-sync

  I want to verify that device check-sync works

  Scenario: In sync
    Given I have initialized the world
    Given I mock check-sync to return "in-sync"
    When The check-sync method is called
    Then I did not receive an exception
    Then The resulting check-sync response result is "in-sync"

  Scenario: Out of sync
    Given I have initialized the world
    Given I mock check-sync to return "out-of-sync"
    When The check-sync method is called
    Then I did not receive an exception
    Then The resulting check-sync response result is "out-of-sync"