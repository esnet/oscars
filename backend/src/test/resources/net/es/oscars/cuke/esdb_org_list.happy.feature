@EsdbOrgSteps
Feature: Retrieve organization list from ESDB

  Scenario: Retrieve organization types from ESDB graphql

    When I retrieve the organization types from ESDB
    Then I did not receive an exception
    Then The number of organization types was greater than 0
    Then There is an organization type with the name "Project"
    Then There is an organization type with the name "Vendor"
    Then There is an organization type with the name "ESnet Site"

  Scenario: Retrieve organizations from ESDB graphql
    When I retrieve organizations from ESDB with "Project" type
    Then I did not receive an exception
    Then The number of organizations was greater than 0
