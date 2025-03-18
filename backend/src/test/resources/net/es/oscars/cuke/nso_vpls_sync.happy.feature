@NsoVplsSyncSteps
Feature: synchronize NSO service state to OSCARS state (Happy Path)

  I want to verify that NSO service state is synchronized to the OSCARS state (Happy Path)

  # Happy path
  Scenario: Read NSO VPLS service state, make decisions about add / delete / redeploy
    Given I have initialized the world
    Given The NSO VPLS service state is loaded
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.json"
    Given The NSO VPLS service state has 139 instances


    # All the various evaluation functions live in NsoStateSyncer

    # AAAA should not exist, mark as add
    Given The VPLS instance "AAAA" is not present in the NSO VPLS service state
    When I evaluate VPLS "AAAA"
    Then VPLS "AAAA" is marked as "add"

    # BBBB should exist, mark for delete
    Given The VPLS instance "BBBB" is present in the NSO VPLS service state
    When I evaluate VPLS "BBBB"
    Then VPLS "BBBB" is marked as "delete"

    # CCCC should exist, but is mismatched with our copy of CCCC, mark for redeploy
    When I evaluate VPLS "CCCC"
    Then VPLS "CCCC" is marked as "redeploy"

    # DDDD should exist, mark for no-op
    Given The VPLS instance "DDDD" is present in the NSO VPLS service state
    When I evaluate VPLS "DDDD"
    Then VPLS "DDDD" is marked as "no-op"

  # Happy path
  Scenario: Read NSO VPLS service state, make decisions about multiple add / delete / redeploys
    Given I have initialized the world
    Given The NSO VPLS service state is loaded
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.json"
    Given The NSO VPLS service state has 139 instances

    # Evaluate AAAA and AAA2, both add.
    When I evaluate VPLS "AAAA"
    When I evaluate VPLS "AAA2"
    Then The list of VPLS service instances marked "add" equals "http/nso.esnet-vpls.add-these-2.json"

    # Evaluate BBBB and BBB2, both delete
    When I evaluate VPLS "BBBB"
    When I evaluate VPLS "BBB2"
    Then The list of VPLS service instances marked "delete" equals "http/nso.esnet-vpls.delete-these.json"

    # Evaluate CCCC and CCC2, both delete
    When I evaluate VPLS "CCCC"
    When I evaluate VPLS "CCC2"
    Then The list of VPLS service instances marked "redeploy" equals "http/nso.esnet-vpls.redeploy-these.json"

    # Evaluate DDDD and DDD2, both no-op
    When I evaluate VPLS "DDDD"
    When I evaluate VPLS "DDD2"
    Then The list of VPLS service instances marked "no-op" equals "http/nso.esnet-vpls.no-op-these.json"

  #Happy path
  Scenario: Modify NSO VPLS service state with single adds
    Given I have initialized the world
    Given The NSO VPLS service state is loaded
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active-with-add.json"

    # Add the VPLS "AAAA" to service state without it, should be an "add" operation
    Given The VPLS instance "AAAA" is not present in the NSO VPLS service state
    When I add VPLS instance "AAAA"
    When I evaluate VPLS "AAAA"
    Then VPLS "AAAA" is marked as "add"
    Then The NSO VPLS service is synchronized
    Then The NSO VPLS service state now has 140 instances

    # VPLS "AAAA" already exists, adding it again should be a "no-op"
    Given The VPLS instance "AAAA" is present in the NSO VPLS service state
    When I add VPLS instance "AAAA"
    When I evaluate VPLS "AAAA"
    Then VPLS "AAAA" is marked as "no-op"
    Then The NSO VPLS service is synchronized
    Then The NSO VPLS service state now has 140 instances

  # Happy path
  Scenario: Modify NSO VPLS service state with single deletes

    Given I have initialized the world
    Given The NSO VPLS service state is loaded
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active-with-delete.json"

    # Delete the VPLS "BBBB"
    Given The VPLS instance "BBBB" is present in the NSO VPLS service state
    When I delete VPLS instance "BBBB"
    When I evaluate VPLS "BBBB"
    Then VPLS "BBBB" is marked as "delete"
    Then The NSO VPLS service is synchronized
    Then The NSO VPLS service state has 139 instances

  # Happy path
  Scenario: Modify NSO VPLS service state with mismatch for single redeploys
    Given I have initialized the world
    Given The NSO VPLS service state is loaded
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active-with-mismatch.json"
    Given The VPLS instance "CCCC" is present in the NSO VPLS service state

    When I evaluate VPLS "CCCC"

    Then VPLS "CCCC" is marked as "redeploy"

    Then The NSO VPLS service is synchronized
    Then The VPLS instance "CCCC" matches "http/nso.esnet-vpls.connections-with-syncd-CCCC.json"

    # Happy path
  Scenario: Modify NSO VPLS service state without mismatch for single redeploys
    Given I have initialized the world
    Given The NSO VPLS service state is loaded
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.json"
    Given The VPLS instance "CCCC" is present in the NSO VPLS service state
    When I evaluate VPLS "CCCC"
    Then VPLS "CCCC" is marked as "no-op"
    Then The NSO VPLS service is synchronized
    Then The VPLS instance "CCCC" matches "http/nso.esnet-vpls.connections-with-syncd-CCCC.json"

#
#
#  Scenario: Batch modify NSO VPLS service state with a big patch that applies one add, one delete, one redeploy all together
#    Given I have initialized the world
#    Given I load the NSO VPLS service state
#    Then The NSO VPLS service state has 139 instances
#    When I apply VPLS service patch from "vpls-patch.json"
#    Then The NSO VPLS service state has 139 instances
#    Given The VPLS instance "AAAA" is present in the NSO VPLS service state
#    Then The VPLS instance "AAAA" matches "AAAA.json"
#    Given The VPLS instance "BBBB" is not present in the NSO VPLS service state
#    Then The VPLS instance "CCCC" matches "CCCC.json"
#    Then If I evaluate vpls "AAAA" I will decide that I need to "no-op"
#    Then If I evaluate vpls "BBBB" I will decide that I need to "no-op"
#    Then If I evaluate vpls "CCCC" I will decide that I need to "no-op"


