@NsoVplsSyncSteps
Feature: Synchronize NSO service state to OSCARS state (Unhappy Path)
  I want to verify that NSO service state is synchronized to the OSCARS state (Unhappy Path)

  # Unhappy path
  Scenario: Read NSO VPLS service state, make decisions about add (Unhappy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 133 instances
    Given The world is expecting an exception

    # AAAA does not exist! Attempt to add, but CANNOT. Exception thrown.
    Given The VPLS instance "AAAA" is not present in the NSO VPLS service state
    Given I did not add "AAAA"
    When I evaluate VPLS "AAAA"
    Then I did receive an exception

  Scenario: Read NSO VPLS service state, make decisions about delete (Unhappy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 133 instances
    Given The world is expecting an exception

    # BBBB should exist! Attempt to delete, but CANNOT. Exception thrown.
    Given The VPLS instance "BBBB" is not loaded
    When I mark VPLS instance "BBBB" with "delete"
    Then I did receive an exception

  Scenario: Read NSO VPLS service state, make decisions about delete (Unhappy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 133 instances
    Given The world is expecting an exception

    # Attempt to mark "CCCC" for redeploy, but CANNOT. Exception thrown.
    Given The VPLS instance "CCCC" is not loaded
    When I evaluate VPLS "CCCC"
    Then I did receive an exception

  Scenario: Read NSO VPLS service state, make decisions about no-op (Unhappy Path)
    Given I have initialized the world
    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 133 instances
    Given The world is expecting an exception
    # DDDD should exist and is matched w/ NSO state! Attempt to mark for no-op, but CANNOT. Exception thrown.
    Given The VPLS instance "DDDD" is not loaded
    When I mark VPLS instance "DDDD" with "no-op"
    Then I did receive an exception
