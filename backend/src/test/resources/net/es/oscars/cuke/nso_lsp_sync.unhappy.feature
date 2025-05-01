@NsoLspSyncSteps
Feature: synchronize NSO service state to OSCARS state, LSP (Unhappy path)

  I want to verify that NSO service state is synchronized to the OSCARS state.
  Evaluation mechanism should automatically mark LSPs as one of "add", "delete", "redeploy", or "no-op".

  Evaluate -> Mark -> Synchronize.

  Scenario: Read NSO's LSP service state, no changes, attempt synchronize with a 'clean' state.
    Given I have initialized the world
    Given I have retrieved the NSO LSPs
    Given The NSO LSP service state is loaded
    Given The NSO LSP service state has 536 instances


    When I perform an LSP synchronization

    Then The NSO LSP service is not synchronized