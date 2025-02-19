@unit
Feature: schedules logic

  I want to verify that my schedule business logic works

  Scenario: Basic schedule overlapping
    Given I have initialized the world

    Given I clear all schedules
    Given I add these schedules
      | A | 10 | 40 | HELD |
      | B | 50 | 90 | HELD |
      | C | 30 | 60 | RESERVED |
#    Then a schedule between 0 and 5 does not overlap
#    Then a schedule between 95 and 100 does not overlap
#    Then a schedule between 5 and 40 does overlap
#      | A |
#      | C |
#    Then a schedule between 5 and 55 does overlap
#      | A |
#      | B |
#      | C |
#    Then a schedule between 45 and 100 does overlap
#      | B |
#      | C |
#    Then a schedule between 70 and 100 does overlap
#      | B |
#    Then a schedule between 15 and 25 does overlap
#      | A |

    Then I did not receive an exception