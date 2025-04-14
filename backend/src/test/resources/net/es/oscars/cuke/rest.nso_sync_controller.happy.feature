@RestNsoSync
Feature: Verify the NsoSyncController endpoint

  Scenario: Get the NSO state
    Given the client executes "GET" on "/api/nso-sync"
    When the client receives the response
    Then the client receives a status code of 200
    And the client receives the payload "/http/rest.oscars.list.all-vpls.json"

  Scenario: Get certain vcIDs from the NSO state
    Given the client executes "GET" on "/api/nso-sync/7000"
    When the client receives the response
    Then the client receives a status code of 200
    And the client receives the payload "/http/rest.oscars.list.one-vpls.json"

  Scenario: Get multiple vcIDs from the NSO state
    Given the client executes "GET" on "/api/nso-sync/7000,7003"
    When the client receives the response
    Then the client receives a status code of 200
    And the client receives the payload "/http/rest.oscars.list.two-vpls.json"