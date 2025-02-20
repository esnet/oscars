@unit
Feature: topology deserialization

  I want to verify that i can load and persist topology

  Scenario: Empty topology loading
    Given I have initialized the world
    Given I clear the topology
    Then the current topology is empty
    Given I load topology from "resources/topology/empty.json"
    Then I did not receive an exception


#  Scenario: Two routers loading
#    Given I have initialized the world
#    Given I clear the topology
#    Given I load topology from "config/test/topo/two_routers.json" and "config/test/topo/adj_a_b_mpls.json"
#    When I merge the new topology
#    Then the "devices" repository has 2 entries
#    Then the "ports" repository has 4 entries
#    Then the "adjacencies" repository has 2 entries
#    Then I did not receive an exception


#  Scenario: Router and switch loading
#    Given I have initialized the world
#    Given I clear the topology
#    Given I load topology from "config/test/topo/router_and_switch.json" and "config/test/topo/adj_a_b_eth.json"
#    When I merge the new topology
#    Then the "devices" repository has 2 entries
#    Then the "ports" repository has 4 entries
#    Then the "adjacencies" repository has 2 entries
#    Then I did not receive an exception