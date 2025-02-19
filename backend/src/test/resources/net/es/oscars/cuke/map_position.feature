@unit
Feature: map positions

  I want to verify that I pass the correct map coordinates

  Scenario: All positions are different
    Given I load my positionMap from "config/test/map_positions/one.json"
    Given I load devices from "config/test/map_positions/one_devices.json"
    Then I can verify my results
    Then I did not receive an exception

  Scenario: Multiple positions with same keys
    Given I load my positionMap from "config/test/map_positions/two.json"
    Given I load devices from "config/test/map_positions/two_devices.json"
    Then I can verify my results
    Then I did not receive an exception

  Scenario: When one device matches multiple keys
    Given I load my positionMap from "config/test/map_positions/three.json"
    Given I load devices from "config/test/map_positions/three_devices.json"
    Then I can verify my results
    Then I did not receive an exception