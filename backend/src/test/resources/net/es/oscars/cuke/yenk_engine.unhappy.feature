@unit
Feature: Do not calculate path from A to Z with the Yenk engine (Unhappy Path)

  I want to verify that I cannot calculate a non-existent from A to path Z with Yenk engine

  Scenario: Do not calculate a path from A to Z with the Yenk engine
    Given I have initialized the world
    Given I instantiate the engine by loading topology from "topo/esnet.json"
    When The paths are calculated from path "a" with device urn "wash-cr6" with port "wash-cr6:2/1/c28/1" to path "z" with device urn "jlab205-cr6" with port "jlab205-cr6:1/1/c2/1"
    Then I did not receive an exception
    Then I did not receive a shortest path