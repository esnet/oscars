@NsoVplsSyncSteps
Feature: synchronize NSO service state to OSCARS state (Unhappy Path)
  I want to verify that NSO service state is synchronized to the OSCARS state (Unhappy Path)

  # Unhappy path
  Scenario: Modify NSO VPLS service state without mismatch for single redeploys
    Given I have initialized the world
    Given The NSO VPLS service state is loaded
    Given The list of active OSCARS connections are loaded from "http/nso.esnet-vpls.connections-active.json"
    Given The VPLS instance "CCCC" is present in the NSO VPLS service state
    When I evaluate VPLS "CCCC"
    Then VPLS "CCCC" is marked as "no-op"
    Then The NSO VPLS service is synchronized
    Then The VPLS instance "CCCC" matches "http/nso.esnet-vpls.connections-with-syncd-CCCC.json"