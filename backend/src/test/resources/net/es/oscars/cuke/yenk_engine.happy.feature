@unit
Feature: Calculate path from A to Z with the Yenk engine (Happy Path)

  I want to verify that I can calculate a path from A to Z with Yenk engine

  Scenario: Calculate a path from A to Z with the Yenk engine
    Given I have initialized the world
    Given I instantiate the engine by loading topology from "topo/esnet.json"
    When The paths are calculated from path "a" with device urn "wash-cr6" with port "wash-cr6:2/1/c28/1" to path "z" with device urn "atla-cr6" with port "atla-cr6:2/1/c28/1"
    Then I did not receive an exception
    Then I did receive a PceResponse
    Then I did get a populated AZ and ZA ero list
    Then I did get valid entries in AZ and ZA ero lists