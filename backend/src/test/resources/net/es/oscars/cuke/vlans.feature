#@unit
#Feature: vlans logic
#
#  I want to verify that my vlans business logic works
#
#  Scenario: Basic availability calculation
#    Given I have initialized the world
#    Given I set this topology baseline
#      | A:1 | 10:50 |
#      | A:2 | 20:30 |
#    Given I set these eternal vlan reservations
#      | A:1 | 10 |
#      | A:1 | 15 |
#      | A:1 | 50 |
#      | A:2 | 20 |
#      | A:2 | 21 |
#      | A:2 | 22 |
#    Then the available vlans for "A:1" are "11:14,16:49"
#    Then the available vlans for "A:2" are "23:30"
#
#    Then I did not receive an exception