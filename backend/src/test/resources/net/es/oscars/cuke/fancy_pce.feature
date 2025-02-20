@unit
Feature: fancy PCE functionality

  I want to verify that I can do fancy pathfinding

  Scenario: Two routers
    Given I have initialized the world
    Given I clear the topology
    Given I load topology from "topo/esnet.json"
    When I merge the new topology
    Given I update the topology URN map after import
#    When I ask for all paths from "aofa-cr5" to "chic-cr5" with a relaxation radius of 12
    Then I did not receive an exception