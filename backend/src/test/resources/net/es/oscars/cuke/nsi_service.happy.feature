@NsiServiceSteps
@NsiServiceStepsHappy
Feature: Run NsiService class methods (Happy)

  I want to verify NsiService can make a reservation

  Scenario: NSI Service makes a connection reservation (with optional projectId field)
    Given The NSI Service class is instantiated
    When The NSI Service submits a reservation for OSCARS connection ID "ABCD", global reservation ID "GLOBALID", NSI connection ID "RES1", project ID "abcd-1234-efgh-5678"
    Then The connection has a projectId value
    Then The NSI Service made the reservation successfully.

  Scenario: NSI Service makes a connection reservation (without optional projectId field)
    Given The NSI Service class is instantiated
    When The NSI Service submits a reservation for OSCARS connection ID "ABCD", global reservation ID "GLOBALID", NSI connection ID "RES1"
    Then the connection does not have a projectId value
    Then The NSI Service made the reservation successfully.

  Scenario: NSI Service provisions a connection
    Given The NSI Service has a reserved connection
    When The NSI Service submits a provision request for a reserved connection
    Then The NSI Service made the provision request successfully.

  Scenario: NSI Service releases a connection
    Given The NSI Service class has a provisioned connection
    When The NSI Service submits a release request for a provisioned connection
    Then The NSI Service made the release request successfully.