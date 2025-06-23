@NsiServiceSteps
@NsiServiceStepsHappy
Feature: Run NSI Service (NsiService) class methods

  I want to verify NsiService can make a reservation

  Scenario: NSI Service makes a connection reservation
    Given The NSI Service class is instantiated
    When The NSI Service submits a reservation for NSA "RES1", connection ID "OSCARS", global reservation ID "GLOBALID", NSI connection ID "RES1", OSCARS connection ID "OSCARS"
    Then The NSI Service made the reservation successfully.