@NsoVplsSyncSteps
Feature: Synchronize NSO service state to OSCARS state (Happy Path)

  I want to verify that NSO service state is synchronized to the OSCARS state (Happy Path).
  Evaluation mechanism should automatically mark VPLS as one of "add", "delete", "redeploy", or "no-op".

  Evaluate -> Mark -> Synchronize.

  # Happy path
  Scenario: Read NSO VPLS service state, make decisions about add / delete / redeploy (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.json"
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 139 instances


    # All the various evaluation functions live in NsoVplsStateSyncer

    # AAAA should NOT exist, mark as add
    Given The VPLS instance "AAAA" is not present in the NSO VPLS service state
    When I evaluate VPLS "AAAA"
    Then VPLS "AAAA" is marked as "add"

    # BBBB should exist, mark for delete
    Given The VPLS instance "BBBB" is present in the NSO VPLS service state
    When I evaluate VPLS "BBBB"
    Then VPLS "BBBB" is marked as "delete"

    # CCCC should exist and IS in sync with NSO state, mark for redeploy
    Given The VPLS instance "CCCC" is present in the NSO VPLS service state
    When I evaluate VPLS "CCCC"
    Then VPLS "CCCC" is marked as "redeploy"

    # DDDD should exist and IS NOT in sync with NSO state, mark for no-op
    Given The VPLS instance "DDDD" is present in the NSO VPLS service state
    When I evaluate VPLS "DDDD"
    Then VPLS "DDDD" is marked as "no-op"

  # Happy path
  Scenario: Read NSO VPLS service state, make decisions about multiple add / delete / redeploys (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.json"
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 139 instances

    # Evaluate AAAA and AAA2, sync should add both.
    When I evaluate VPLS "AAAA"
    When I evaluate VPLS "AAA2"
    Then The list of VPLS service instances marked "add" equals "http/nso.esnet-vpls.add-these.json"

    # Evaluate BBBB and BBB2, sync should delete both
    When I evaluate VPLS "BBBB"
    When I evaluate VPLS "BBB2"
    Then The list of VPLS service instances marked "delete" equals "http/nso.esnet-vpls.delete-these.json"

    # Evaluate CCCC and CCC2, sync should redeploy both
    When I evaluate VPLS "CCCC"
    When I evaluate VPLS "CCC2"
    Then The list of VPLS service instances marked "redeploy" equals "http/nso.esnet-vpls.redeploy-these.json"

    # Evaluate DDDD and DDD2, sync should no-op both
    When I evaluate VPLS "DDDD"
    When I evaluate VPLS "DDD2"
    Then The list of VPLS service instances marked "no-op" equals "http/nso.esnet-vpls.no-op-these.json"

  # Happy path
  Scenario: Modify NSO VPLS service state with single adds (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active-with-add.json"
    Given The NSO VPLS service state is loaded

    # Add the VPLS "AAAA" to service state without it, should be an "add" operation
    Given The VPLS instance "AAA2" is not present in the NSO VPLS service state
    When I add VPLS instance "AAA2"
    Then VPLS "AAA2" is marked as "add"
    Then The NSO VPLS service is synchronized
    Then The NSO VPLS service state now has 140 instances

    # VPLS "AAAA" already exists, adding it again should be a "no-op"
    Given The VPLS instance "AAAA" is not present in the NSO VPLS service state
    When I add VPLS instance "AAAA"
    Then The VPLS instance "AAAA" is present in the NSO VPLS service state
    Then VPLS "AAAA" is marked as "no-op"
    Then The NSO VPLS service is synchronized
    Then The NSO VPLS service state now has 140 instances

  # Happy path
  Scenario: Modify NSO VPLS service state with single deletes (Happy Path)

    Given I have initialized the world
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active-with-delete.json"
    Given The NSO VPLS service state is loaded

    # Delete the VPLS "BBBB"
    Given The VPLS instance "BBBB" is present in the NSO VPLS service state
    When I delete VPLS instance "BBBB"
    Then VPLS "BBBB" is marked as "delete"
    Then The NSO VPLS service is synchronized
    Then The NSO VPLS service state now has 139 instances

  # Happy path
  Scenario: Modify NSO VPLS service state with mismatch for single redeploys (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active-with-mismatch.json"
    Given The NSO VPLS service state is loaded

    # Redeploy the VPLS "CCCC"
    Given The VPLS instance "CCCC" is present in the NSO VPLS service state
    When I redeploy VPLS instance "CCCC"
    Then VPLS "CCCC" is marked as "redeploy"
    Then The NSO VPLS service is synchronized
    Then The VPLS instance "CCCC" matches "http/nso.esnet-vpls.connections-with-syncd-CCCC.json"

  # Happy path
  Scenario: Modify NSO VPLS service state without mismatch for single no-op (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.json"
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 139 instances

    # No-op the VPLS "DDDD"
    Given The VPLS instance "DDDD" is present in the NSO VPLS service state
    When I no-op VPLS "DDDD"
    Then VPLS "DDDD" is marked as "no-op"
    Then The NSO VPLS service is synchronized
    Then The VPLS instance "DDDD" matches "http/nso.esnet-vpls.connections-with-syncd-DDD.json"


  # Happy path
  Scenario: Batch modify NSO VPLS service state with a big patch that applies one add, one delete, one redeploy all together (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.json"
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 139 instances

    # Apply batch operations as a patch.
    When I apply VPLS service patch from "http/nso.esnet-vpls.vpls-patch.json"

    Then VPLS "AAAA" is marked as "add"
    Then VPLS "BBBB" is marked as "delete"
    Then VPLS "CCCC" is marked as "redeploy"
    Then VPLS "DDDD" is marked as "no-op"
    Then The NSO VPLS service is synchronized
    Then The list of VPLS service instances equals "http/nso.esnet-vpls.batch-no-op.json"

