@unit
Feature: bandwidth logic

  I want to verify that my bandwidth availability calculation math works

  Scenario: Basic availability calculation
    Given I have initialized the world
    Given I set this "INGRESS" bandwidth baseline
      | A:1 | 100 |
      | A:2 | 100 |
    Given I set these "INGRESS" bandwidth reservations
#     | urn | bw | beg | end |
      | A:1 | 10 | 100 | 250 |
      | A:1 | 50 | 150 | 170 |
      | A:1 | 10 | 190 | 300 |
      | A:1 | 20 | 210 | 300 |
      | A:2 | 10 | 100 | 200 |
      | A:2 | 20 | 200 | 300 |
# note A:2 has a beg and an end at instant 200; neither reservation
# can be said to be active at that particular instant
    Then the available "INGRESS" bandwidth for "A:1" at 0 is 100
    Then the available "INGRESS" bandwidth for "A:1" at 110 is 90
    Then the available "INGRESS" bandwidth for "A:1" at 160 is 40
    Then the available "INGRESS" bandwidth for "A:1" at 180 is 90
    Then the available "INGRESS" bandwidth for "A:1" at 200 is 80
    Then the available "INGRESS" bandwidth for "A:1" at 220 is 60
    Then the available "INGRESS" bandwidth for "A:1" at 260 is 70
    Then the available "INGRESS" bandwidth for "A:1" at 310 is 100
    Then the available "INGRESS" bandwidth for "A:2" at 0 is 100
    Then the available "INGRESS" bandwidth for "A:2" at 150 is 90
    Then the available "INGRESS" bandwidth for "A:2" at 200 is 100
    Then the available "INGRESS" bandwidth for "A:2" at 201 is 80
    Then the available "INGRESS" bandwidth for "A:2" at 299 is 80
    Then the available "INGRESS" bandwidth for "A:2" at 300 is 100


    Then the overall available "INGRESS" bw for "A:1" is 40
    Then the overall available "INGRESS" bw for "A:2" is 80

    Then I did not receive an exception