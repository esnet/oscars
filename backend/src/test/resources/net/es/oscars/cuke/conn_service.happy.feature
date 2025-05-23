@NsoProxySteps
Feature: OSCARS NSI Connection Service, validation

  I want to validate a connection as either valid (true) or not valid (false).

  # Validate global connection params
  Scenario: Validate global connection params
    Given The connection ID is set to "" and the connection mode is set to ""
    # check the connection ID.
    When The connection ID is set to ""
    When The connection ID is validated
    Then The Connection ID is valid

    # check the connection MTU.
    When The MTU is set to ""
    When The MTU is validated
    Then The MTU is valid

    # check description.
    When The description is set to ""
    When The description is validated
    Then The description is valid

    # Connection ID, connection MTU, and description are all valid.
    Then The global connection params are valid

  # Validate schedule
  Scenario: Validate the schedule
    Given The connection ID is set to "" and the connection mode is set to ""
    Given The global connection was set to valid parameters

    # Check the scheduled begin time
    When The scheduled begin time is set to ""
    When The scheduled begin time is validated
    Then The scheduled begin time is valid

    # Check the scheduled end time
    When The scheduled end time is set to ""
    When The scheduled end time is validated
    Then The scheduled end time is valid

    # The scheduled interval is valid
    Then The scheduled begin time and end time make a valid interval

  # Check resource availability if the schedule makes sense
  Scenario:
    Given The connection ID is set to "" and the connection mode is set to ""
    Given The global connection was set to valid parameters
    Given The scheduled begin time and end time make a valid interval

    # Make maps: urn -> total of what we are requesting to reserve for VLANs and BW
    # ... populate the maps with what we request thru fixtures
    When The maps are populated with what we request through fixtures
    # ... populate the maps with what we request thru pipes (bw only)
    When The maps are polulated with what we request through pipes (BW only)
    # ... compare VLAN maps to what is available
    When The VLAN maps are compared to what is available
    Then The fixtures are valid

    # ... compare map to what is available for BW
    When The map is compared to what is available for BW

    # ... populate Validity for fixtures
    When The Validity for fixtures is populated
    # ... populate Validity for pipes & EROs
    When The Validity for pipes and EROs is populated
    Then The connection is valid (true)