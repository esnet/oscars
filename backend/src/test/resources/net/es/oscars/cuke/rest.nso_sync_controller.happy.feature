@RestNsoSync
@RestNsoSyncHappy
Feature: Verify the NsoSyncController endpoint (Happy Path)

  # HTTP GET
  Scenario: Get the NSO state
    Given the client executes "GET" on "/protected/nso-sync"
    When the client receives the response
    Then the client receives the status code of 200
    Then the client receives the payload "/http/rest.oscars.list.all-vpls.json"

  Scenario: Get certain vcIDs from the NSO state
    Given the client executes "GET" on "/protected/nso-sync/7000"
    When the client receives the response
    Then the client receives the status code of 200
    Then the client receives the payload "/http/rest.oscars.list.one-vpls.json"

  Scenario: Get multiple vcIDs from the NSO state
    Given the client executes "GET" on "/protected/nso-sync/7000,7003"
    When the client receives the response
    Then the client receives the status code of 200
    Then the client receives the payload "/http/rest.oscars.list.two-vpls.json"

  # HTTP POST
  Scenario: Change the NSO state (POST)
    Given the client executes "POST" on "/protected/nso-sync" with payload from "/http/rest.oscars.payload.aaaa.json"
    When the client receives the response
    Then the client receives the status code of 200
    Then the client receives a true synchronization flag

  Scenario: Change the NSO state (POST, multiple)
    Given the client executes "POST" on "/protected/nso-sync" with payload from "/http/rest.oscars.payload.aaaa-aaa2.json"
    When the client receives the response
    Then the client receives the status code of 200
    Then the client receives a true synchronization flag

  # HTTP DELETE
  Scenario: Change the NSO state (DELETE)
    Given the client executes "DELETE" on "/protected/nso-sync/7000"
    When the client receives the response
    Then the client receives the status code of 200
    Then the client receives a true synchronization flag
