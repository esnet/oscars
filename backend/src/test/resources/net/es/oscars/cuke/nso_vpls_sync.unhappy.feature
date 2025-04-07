@NsoVplsSyncSteps
Feature: Synchronize NSO service state to OSCARS state (Unhappy Path)
  I want to verify that NSO service state is synchronized to the OSCARS state (Unhappy Path)

  # Unhappy path
  Scenario: Read NSO VPLS service state, make decisions about add (Unhappy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.unhappy.json"
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 137 instances
    Given The world is expecting an exception

    # AAAA does not exist! Attempt to add, but CANNOT. Exception thrown.
    Given The VPLS instance "AAAA" is not present in the NSO VPLS service state
    Given I did not add "AAAA"
    When I evaluate VPLS "AAAA"
    Then I did receive an exception

  Scenario: Read NSO VPLS service state, make decisions about delete (Unhappy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.unhappy.json"
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 137 instances
    Given The world is expecting an exception

    # BBBB should exist! Attempt to delete, but CANNOT. Exception thrown.
    Given The VPLS instance "BBBB" is not loaded
    When I mark VPLS instance "BBBB" with "delete"
    Then I did receive an exception
#
#    # CCCC should exist and is mismatched w/ NSO state! Attempt to mark for redeploy, but CANNOT. Exception thrown.
#    Given The VPLS instance "CCCC" is present in the NSO VPLS service state
#    When I evaluate VPLS "CCCC"
#    Then I did receive an exception
#
#    # DDDD should exist and is matched w/ NSO state! Attempt to mark for no-op, but CANNOT. Exception thrown.
#    Given The VPLS instance "DDDD" is present in the NSO VPLS service state
#    When I evaluate VPLS "DDDD"
#    Then I did receive an exception
#
#  # Unhappy path
#  Scenario: Read NSO VPLS service state, make decisions about multiple add / delete / redeploys (Unhappy Path)
#    Given I have initialized the world
#    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.unhappy.json"
#    Given The NSO VPLS service state is loaded
#    Given The NSO VPLS service state has 139 instances
#
#    # Evaluate AAAA and AAA2, sync cannot add AAAA. Exception thrown.
#    When I evaluate VPLS "AAAA"
#    Then I did receive an exception
#
#    # Evaluate AAAA and AAA2, sync cannot add AAA2. Exception thrown.
#    When I evaluate VPLS "AAA2"
#    Then I did receive an exception
#
#    # Evaluate AAAA and AAA2, sync cannot add BOTH of them. Exception thrown.
#    When I evaluate VPLS "AAAA,AAA2"
#    Then I did receive an exception
#
#    # Evaluate BBBB and BBB2, sync cannot delete BBBB. Exception thrown.
#    When I evaluate VPLS "BBBB"
#    Then I did receive an exception
#
#    # Evaluate BBBB and BBB2, sync cannot delete BBB2. Exception thrown.
#    When I evaluate VPLS "BBB2"
#    Then I did receive an exception
#
#    # Evaluate BBBB and BBB2, sync cannot delete BOTH of them. Exception thrown.
#    When I evaluate VPLS "BBBB,BBB2"
#    Then I did receive an exception
#
#    # Evaluate CCCC and CCC2, sync CANNOT redeploy CCCC. Exception thrown.
#    When I evaluate VPLS "CCCC"
#    Then I did receive an exception
#
#    # Evaluate CCCC and CCC2, sync CANNOT redeploy CCC2. Exception thrown.
#    When I evaluate VPLS "CCC2"
#    Then I did receive an exception
#
#    # Evaluate CCCC and CCC2, sync CANNOT redeploy BOTH of them. Exception thrown.
#    When I evaluate VPLS "CCCC,CCC2"
#    Then I did receive an exception
#
#    # Evaluate DDDD and DDD2, sync CANNOT no-op DDDD. Exception thrown.
#    When I evaluate VPLS "DDDD"
#    Then I did receive an exception
#
#    # Evaluate DDDD and DDD2, sync CANNOT no-op DDD2. Exception thrown.
#    When I evaluate VPLS "DDD2"
#    Then I did receive an exception
#
#    # Evaluate DDDD and DDD2, sync CANNOT no-op BOTH of them. Exception thrown.
#    When I evaluate VPLS "DDDD,DDD2"
#    Then I did receive an exception
#
#  # Unhappy path
#  Scenario: Modify NSO VPLS service state with single adds (Unhappy Path)
#    Given I have initialized the world
#    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active-with-add.unhappy.json"
#    Given The NSO VPLS service state is loaded
#
#    # Add the VPLS "AAA2" to service state without it, but it CANNOT. Exception thrown.
#    Given The VPLS instance "AAA2" is not present in the NSO VPLS service state
#    When I add VPLS instance "AAA2"
#    Then I did receive an exception
#
#    # Add the VPLS "AAAA" to service state where it already exists, but it CANNOT. Exception thrown.
#    Given The VPLS instance "AAAA" is present in the NSO VPLS service state
#    When I add VPLS instance "AAAA"
#    Then I did receive an exception
#
#  # Unhappy path
#  Scenario: Modify NSO VPLS service state with single deletes (Unhappy Path)
#    Given I have initialized the world
#    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active-with-delete.unhappy.json"
#    Given The NSO VPLS service state is loaded
#
#    # Delete the VPLS "BBBB", but CANNOT. Exception thrown.
#    Given The VPLS instance "BBBB" is present in the NSO VPLS service state
#    When I delete VPLS instance "BBBB"
#    Then I did receive an exception
#
#  # Unhappy path
#  Scenario: Modify NSO VPLS service state with mismatch for single redeploys (Unhappy Path)
#    Given I have initialized the world
#    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active-with-mismatch.unhappy.json"
#    Given The NSO VPLS service state is loaded
#
#    # Redeploy the VPLS "CCCC", but CANNOT. Exception thrown.
#    Given The VPLS instance "CCCC" is present in the NSO VPLS service state
#    When I redeploy VPLS instance "CCCC"
#    Then I did receive an exception
#
#  # Unhappy path
#  Scenario: Modify NSO VPLS service state without mismatch for single no-op (Unhappy Path)
#    Given I have initialized the world
#    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.unhappy.json"
#    Given The NSO VPLS service state is loaded
#    Given The NSO VPLS service state has 139 instances
#
#    # No-op the VPLS "DDDD", but CANNOT. Exception thrown.
#    Given The VPLS instance "DDDD" is present in the NSO VPLS service state
#    When I no-op VPLS "DDDD"
#    Then I did receive an exception
#
#  # Unhappy path
#  Scenario: Batch modify NSO VPLS service state with a big patch that applies one add, one delete, one redeploy all together (Unhappy Path)
#    Given I have initialized the world
#    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.unhappy.json"
#    Given The NSO VPLS service state is loaded
#    Given The NSO VPLS service state has 139 instances
#
#    # Apply batch operations as a patch, but CANNOT. Exception thrown.
#    When I apply VPLS service patch from "http/nso.esnet-vpls.vpls-patch.unhappy.json"
#    Then I did receive an exception