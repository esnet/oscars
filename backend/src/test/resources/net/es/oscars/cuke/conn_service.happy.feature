@ConnServiceSteps
@ConnServiceStepsHappy
Feature: OSCARS NSI Connection Service, validation (Happy Path)

  I want to validate a connection as either valid (true) or not valid (false).

  # Validate
  Scenario: Validate the connection
    # Validation connection params
    # check the connection ID.
    Given The connection ID is set to "ABCD" and the connection mode is set to "NEW"

    # SimpleConnection build mode
    Given The build mode is set to "AUTOMATIC"

    # check the connection MTU.
    Given The MTU is set to 9000

    # check description.
    Given The description is set to "This is a test"

    # Validate schedule
    # Check the scheduled begin time
    # Check the scheduled end time
    Given The schedule is set to a valid time

    # The scheduled interval is valid
    # Check resource availability if the schedule makes sense
    # Make maps: urn -> total of what we are requesting to reserve for VLANs and BW
    # ... populate the maps with what we request thru fixtures
    # ... populate the maps with what we request thru pipes (bw only)
    # ... compare VLAN maps to what is available
    # ... compare map to what is available for BW

    # ... populate Validity for fixtures
    # ... populate Validity for pipes & EROs

    # Connection is valid.
    When The connection is validated
    Then The connection is valid