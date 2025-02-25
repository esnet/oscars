@unit
Feature: Calculate path from A to Z with the Yenk engine

  I want to verify that I can calculate a path from A to Z with Yenk engine

  Scenario: Calculate a path from A to Z with the Yenk engine
    Given I have initialized the world
    Given I instantiate the engine by loading topology from "topo/esnet.json"
    When The paths are calculated from "a" to "z"
    Then I did not receive an exception
    Then I did receive a PceResponse