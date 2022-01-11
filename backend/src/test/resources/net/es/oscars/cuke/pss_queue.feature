@unit
Feature: PSS task queueing

  I want to verify that my task queueing logic is correct

  Scenario: Add single task to task queue
    Given I have initialized the world
    Given I clear all sets
    When I add a "BUILD" task for "XYZZY" intending "ACTIVE"
    Then the "WAITING" set has 1 entries
    Then the "RUNNING" set has 0 entries
    Then the "DONE" set has 0 entries
    Then I did not receive an exception

  Scenario: Fully process build
    Given I have initialized the world
    Given I clear all sets
    When I add a "BUILD" task for "XYZZY" intending "ACTIVE"
    When I trigger the queue processor
    Then the "WAITING" set has 0 entries
    Then the "RUNNING" set has 1 entries
    Then the "DONE" set has 0 entries
    When I make all RUNNING tasks complete
    Then the "WAITING" set has 0 entries
    Then the "RUNNING" set has 0 entries
    Then the "DONE" set has 1 entries
    Then I did not receive an exception

  Scenario: Fully process dismantle
    Given I have initialized the world
    Given I clear all sets
    When I add a "BUILD" task for "XYZZY" intending "FINISHED"
    When I trigger the queue processor
    Then the "WAITING" set has 0 entries
    Then the "RUNNING" set has 1 entries
    Then the "DONE" set has 0 entries
    When I make all RUNNING tasks complete
    Then the "WAITING" set has 0 entries
    Then the "RUNNING" set has 0 entries
    Then the "DONE" set has 1 entries
    Then I did not receive an exception

  Scenario: Avoid double-inserting into WAITING set
    Given I have initialized the world
    Given I clear all sets
    When I add a "BUILD" task for "XYZZY" intending "ACTIVE"
    When I add a "BUILD" task for "XYZZY" intending "ACTIVE"
    Then the "WAITING" set has 1 entries
    Then the "RUNNING" set has 0 entries
    Then I did not receive an exception

  Scenario: Avoid inserting already RUNNING task into WAITING set
    Given I have initialized the world
    Given I clear all sets
    When I add a "BUILD" task for "XYZZY" intending "ACTIVE"
    Then the "WAITING" set has 1 entries
    Then the "RUNNING" set has 0 entries
    When I trigger the queue processor
    Then the "WAITING" set has 0 entries
    Then the "RUNNING" set has 1 entries
    When I add a "BUILD" task for "XYZZY" intending "ACTIVE"
    When I trigger the queue processor
    Then the "WAITING" set has 0 entries
    Then the "RUNNING" set has 1 entries
    Then I did not receive an exception

  Scenario: Anti-tasks
    Given I have initialized the world
    Given I clear all sets
    When I add a "BUILD" task for "XYZZY" intending "ACTIVE"
    When I add a "DISMANTLE" task for "XYZZY" intending "WAITING"
    Then the "WAITING" set has 0 entries
    Then the "RUNNING" set has 0 entries
    Then the "DONE" set has 0 entries
    Then I did not receive an exception


