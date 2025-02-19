@unit
Feature: basic PCE functionality

  I want to verify that I can do pathfinding between two routers

  Scenario: Two routers
    Given I have initialized the world
    Given I clear the topology
    Given I load topology from "config/test/topo/two_routers.json" and "config/test/topo/adj_a_b_mpls.json"
    When I merge the new topology
    Given I update the topology URN map after import
    When I ask for a path from "A" to "B" with az: 10 and za: 10
    Then the resulting AZ ERO is:
      | A   |
      | A:2 |
      | B:2 |
      | B   |
    Then I did not receive an exception