@unit
Feature: availability calculations

  I want to verify that I can apply reservations to baseline topology


  Scenario: availability over two routers
    Given I have initialized the world
    Given I clear the topology
    Then the current topology is empty
    Given I load topology from "topo/esnet.json"
    Then the current topology is not empty
#    Given I update the topology URN map after import
#    Given I set these "INGRESS" bandwidth reservations
##     | urn | bw | beg | end |
#      | A:1 | 10 | 100 | 250 |
#      | A:2 | 10 | 100 | 250 |
#      | B:2 | 10 | 100 | 250 |
#      | B:1 | 10 | 100 | 250 |
#    Given I set these eternal vlan reservations
#      | A:1 | 101 |
#      | B:1 | 101 |
#    Then the "INGRESS" bw availability map between 0 and 80 is
##     | urn | in  |
#      | A:1 | 100 |
#      | B:1 | 100 |
#      | A:2 | 100 |
#      | B:2 | 100 |
#      | A:3 | 100 |
#      | B:3 | 100 |
#    Then the "INGRESS" bw availability map between 70 and 300 is
##     | urn | in  |
#      | A:1 | 90  |
#      | B:1 | 90  |
#      | A:2 | 90  |
#      | B:2 | 90  |
#      | A:3 | 100 |
#      | B:3 | 100 |
#    Then the vlan availability map between 0 and 5 is
##     | urn | bw          |
#      | A:1 | 100,102:200 |
#      | B:1 | 100,102:200 |
#      | A:3 | 100:200 |
#      | B:3 | 100:200 |

    Then I did not receive an exception