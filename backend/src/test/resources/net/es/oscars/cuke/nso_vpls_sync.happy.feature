@NsoVplsSyncSteps
Feature: Synchronize NSO service state to OSCARS state (Happy Path)

  I want to verify that NSO service state is synchronized to the OSCARS state (Happy Path).
  Evaluation mechanism should automatically mark VPLS as one of "add", "delete", "redeploy", or "no-op".

  Evaluate -> Mark -> Synchronize.

  # Happy path
  Scenario: Read NSO VPLS service state, make decisions about add / delete / redeploy (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 133 instances

    # All the various evaluation functions live in NsoVplsStateSyncer

    # AAAA should NOT exist, mark as add
    Given The VPLS instance "AAAA" is not present in the NSO VPLS service state
    Given I had added VPLS instance "AAAA" from "http/nso.esnet-vpls.aaaa.json"
    When I evaluate VPLS "AAAA"
    Then The list of VPLS service instances marked "add" has a count of 1
    Then VPLS "AAAA" is marked as "add"

    # BBBB should exist, mark for delete
    Given The VPLS instance "BBBB" is present in the NSO VPLS service state
    Given I had removed VPLS instance "BBBB"
    When I evaluate VPLS "BBBB"
    Then The list of VPLS service instances marked "delete" has a count of 1
    Then VPLS "BBBB" is marked as "delete"

    # CCCC should exist and IS NOT in sync with NSO state, mark for redeploy
    Given The VPLS instance "CCCC" is present in the NSO VPLS service state
    # ...changes ingress-mbps and egress-mbps from 5000 to 10000
    Given I had changed VPLS instance "CCCC" to "CCCC" from "http/nso.esnet-vpls.cccc.json"
    When I evaluate VPLS "CCCC"
    Then The list of VPLS service instances marked "redeploy" has a count of 1
    Then VPLS "CCCC" is marked as "redeploy"

    # DDDD should exist and IS in sync with NSO state, mark for no-op
    Given The VPLS instance "DDDD" is present in the NSO VPLS service state
    When I evaluate VPLS "DDDD"
    Then The list of VPLS service instances marked "no-op" has a count of 131
    Then VPLS "DDDD" is marked as "no-op"

  # Happy path
  Scenario: Read NSO VPLS service state, make decisions about multiple add / delete / redeploys (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 133 instances

    # Evaluate AAAA and AAA2, sync should add both.
    Given I had added VPLS instance "AAAA" from "http/nso.esnet-vpls.aaaa.json"
    Given I had added VPLS instance "AAA2" from "http/nso.esnet-vpls.aaaa.json"
    When I evaluate VPLS "AAAA"
    When I evaluate VPLS "AAA2"
    Then The list of VPLS service instances marked "add" has a count of 2

    # Evaluate BBBB and BBB2, sync should delete both
    Given I had removed VPLS instance "BBBB"
    Given I had removed VPLS instance "BBB2"
    When I evaluate VPLS "BBBB"
    When I evaluate VPLS "BBB2"
    Then The list of VPLS service instances marked "delete" has a count of 2

    # Evaluate CCCC and CCC2, sync should redeploy both
    # ...changes ingress-mbps and egress-mbps from 5000 to 10000
    Given I had changed VPLS instance "CCCC" to "CCCC" from "http/nso.esnet-vpls.cccc.json"
    # ...changes ingress-mbps and egress-mbps from 1 to 50
    Given I had changed VPLS instance "CCC2" to "CCC2" from "http/nso.esnet-vpls.cccc.json"
    When I evaluate VPLS "CCCC"
    When I evaluate VPLS "CCC2"
    Then The list of VPLS service instances marked "redeploy" has a count of 2

    # Evaluate DDDD and DDD2, sync should no-op both
    When I evaluate VPLS "DDDD"
    When I evaluate VPLS "DDD2"
    Then The list of VPLS service instances marked "no-op" has a count of 129

  Scenario: Read NSO VPLS service state, make decisions about redeploying with diffs
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded

    # Evaluate CCCC when metadata is missing: should not redeploy
    Given I had changed VPLS instance "CCCC" to "CCCC" from "http/nso.esnet-vpls.cccc-meta-removed.json"
    When I evaluate VPLS "CCCC"
    Then The list of VPLS service instances marked "redeploy" has a count of 0

    # Evaluate CCCC when order of device entries has changed - should not redeploy
    Given I had changed VPLS instance "CCCC" to "CCCC" from "http/nso.esnet-vpls.cccc-order-diff.json"
    When I evaluate VPLS "CCCC"
    Then The list of VPLS service instances marked "redeploy" has a count of 0


  # Happy path
  Scenario: Modify NSO VPLS service state with single adds (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 133 instances

    # Add the VPLS "AAAA" to service state without it, should be an "add" operation
    Given The VPLS instance "AAAA" is not present in the NSO VPLS service state
    Given I had added VPLS instance "AAAA" from "http/nso.esnet-vpls.aaaa.json"
    When I mark VPLS instance "AAAA" with "add"
    Then VPLS "AAAA" is marked as "add"
    Then The list of VPLS service instances marked "add" has a count of 1
    When I perform a synchronization
    Then The NSO VPLS service is synchronized

  # Happy path
  Scenario: Modify NSO VPLS service state with single deletes (Happy Path)

    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 133 instances

    # Delete the VPLS "BBBB"
    Given The VPLS instance "BBBB" is present in the NSO VPLS service state
    Given I had deleted VPLS instance "BBBB"
    When I mark VPLS instance "BBBB" with "delete"
    Then VPLS "BBBB" is marked as "delete"
    When I perform a synchronization
    Then The list of VPLS service instances marked "delete" has a count of 1
    Then The NSO VPLS service is synchronized


  # Happy path
  Scenario: Modify NSO VPLS service state with mismatch for single redeploys (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded

    # Redeploy the VPLS "CCCC"
    Given The VPLS instance "CCCC" is present in the NSO VPLS service state
    Given I had marked "CCCC" with "redeploy"
    Then VPLS "CCCC" is marked as "redeploy"
    When I perform a synchronization
    Then The list of VPLS service instances marked "redeploy" has a count of 1
    Then The NSO VPLS service is synchronized

  # Happy path
  Scenario: Modify NSO VPLS service state without mismatch for single no-op (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 133 instances

    # No-op the VPLS "DDDD"
    Given The VPLS instance "DDDD" is present in the NSO VPLS service state
    Given I had marked "DDDD" with "no-op"
    When I perform a synchronization
    Then The NSO VPLS service is synchronized
    Then The list of VPLS service instances marked "no-op" has a count of 133


  # Happy path
  Scenario: Batch modify NSO VPLS service state with a big patch that applies one add, one delete, one redeploy all together (Happy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 133 instances

    # Apply batch operations as a patch.
#    When I apply VPLS service patch from "http/nso.esnet-vpls.vpls-patch.json"

    Given I had added VPLS instance "AAAA" from "http/nso.esnet-vpls.aaaa.json"
    Given I had marked "BBBB" with "delete"
    Given I had marked "CCCC" with "redeploy"

    When I perform a synchronization

    Then VPLS "AAAA" is marked as "add"
    Then VPLS "BBBB" is marked as "delete"
    Then VPLS "CCCC" is marked as "redeploy"
    Then VPLS "DDDD" is marked as "no-op"
    Then The list of VPLS service instances marked "add" has a count of 1
    Then The list of VPLS service instances marked "delete" has a count of 1
    Then The list of VPLS service instances marked "redeploy" has a count of 1

    Then The NSO VPLS service is synchronized
    Then The list of VPLS service instances marked "no-op" has a count of 131

