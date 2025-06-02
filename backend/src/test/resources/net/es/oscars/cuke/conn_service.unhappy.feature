@ConnServiceSteps
@ConnServiceStepsUnhappy
Feature: OSCARS NSI Connection Service, validation (Unhappy Path)

  I want to validate a connection as either valid (true) or not valid (false).

  # Validate
  Scenario: Validate the connection, Invalid connection ID format
    # Validation connection params
    # check the connection ID (Invalid string size).
    Given The connection ID is set to "ABC" and the connection mode is set to "NEW"
    Given The build mode is set to "AUTOMATIC"
    Given The MTU is set to 9000
    Given The description is set to "This is a test"
    Given The schedule is set to a valid time

    # Connection is not valid.
    When The connection is validated
    Then The connection is not valid
  Scenario: Validate the connection, Invalid connection ID format
    # Validation connection params
    # check the connection ID (Invalid MTU size).
    Given The connection ID is set to "ABCD" and the connection mode is set to "NEW"
    Given The build mode is set to "AUTOMATIC"
    # application.properties setting, min is 1500, max is 9000. Default is 9000
    Given The MTU is set to 1
    Given The description is set to "This is a test"
    Given The schedule is set to a valid time

    # Connection is not valid.
    When The connection is validated
    Then The connection is not valid
  Scenario: Validate the connection, Invalid description string (empty)
    # Validation connection params
    # check the connection ID (Invalid description string).
    Given The connection ID is set to "ABCD" and the connection mode is set to "NEW"
    Given The build mode is set to "AUTOMATIC"
    Given The MTU is set to 9000
    # Should not allow an empty description
    Given The description is set to ""
    Given The schedule is set to a valid time

    # Connection is not valid.
    When The connection is validated
    Then The connection is not valid
  Scenario: Validate the connection, invalid schedule
    # Mbps reservation is only 1000 Mbps... attempt to reserve 1001 Mbps. Should fail.
    # Validation connection params
    # check the connection ID .
    Given The connection ID is set to "ABCD" and the connection mode is set to "NEW"
    Given The build mode is set to "AUTOMATIC"
    Given The MTU is set to 9000
    Given The description is set to "This is a test"
    Given The schedule is set to an invalid time

    # Connection is not valid.
    When The connection is validated
    Then The connection is not valid
  Scenario: Validate the connection, invalid Mbps reservation
    # Mbps reservation is only 1000 Mbps... attempt to reserve 1001 Mbps. Should fail.
    Given The connection attempts to reserve 1000 Mbps in, 1000 Mbps out, 1000 Mbps from a to z, 1000 Mbps from z to a, 1001 Mbps set
    # Validation connection params
    # check the connection ID .
    Given The connection ID is set to "ABCD" and the connection mode is set to "NEW"
    Given The build mode is set to "AUTOMATIC"
    Given The MTU is set to 9000
    Given The description is set to "This is a test"
    Given The schedule is set to a valid time

    # Connection is not valid.
    When The connection is validated
    Then The connection is not valid
  Scenario: Validate the connection, Invalid connection ID format