@NsoSyncScheduledTaskSteps
Feature: scheduled task to synchronize VPLS and LSP to NSO state
  I want to verify the scheduled task periodically syncs VPLS and LSP data to NSO state.

  Scenario: Periodically synchronize VPLS and LSP state from OSCARS to NSO state as a scheduled task.
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 137 instances

    # AAAA should NOT exist, mark as add
    Given The VPLS instance "AAAA" is not present in the NSO VPLS service state
    Given I had added VPLS instance "AAAA" from "http/nso.esnet-vpls.aaaa.json"
    When I evaluate VPLS "AAAA"
    Then The list of VPLS service instances marked "add" has a count of 1
    Then VPLS "AAAA" is marked as "add"

    Then The scheduled task should be executed at least 1 times within 2 seconds