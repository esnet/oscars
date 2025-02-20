#@unit
#Feature: design deserialization
#
#  I want to verify that i can serialize and persist designs
#
#  Scenario: Basic loading
#    Given I have initialized the world
#    Given I load a design from "designs/simple.json"
#    When I clear the "design" repository
#    Then I can persist the design
#    When I load the design from the repository
#    Then the "design" repository has 1 entries
#    Then the "schedule" repository has 0 entries
#    Then the "fixture" repository has 2 entries
#    Then the "junction" repository has 2 entries
#    Then the "pipe" repository has 1 entries
#    Then the "vlan" repository has 2 entries
#
#    Then I did not receive an exception
#
#
#  Scenario: Invalid design - missing required property
#    Given I have initialized the world
#    Given The world is expecting an exception
#    Given I load a design from "config/test/designs/invalid-missing-property.json"
#    Then I did receive an exception
#
#  Scenario: Invalid design - null required property
#    Given I have initialized the world
#    Given The world is expecting an exception
#    Given I load a design from "config/test/designs/invalid-null-property.json"
#    Then I did receive an exception
#
#  Scenario: Invalid design - unknown property
#    Given I have initialized the world
#    Given The world is expecting an exception
#    Given I load a design from "config/test/designs/invalid-unknown-property.json"
#    Then I did receive an exception
#
#  Scenario: Invalid design - missing ref
#    Given I have initialized the world
#    Given The world is expecting an exception
#    Given I load a design from "config/test/designs/invalid-missing-ref.json"
#    Then I did receive an exception
#
#  Scenario: Invalid design - bad ref
#    Given I have initialized the world
#    Given The world is expecting an exception
#    Given I load a design from "config/test/designs/invalid-bad-ref.json"
#    Then I did receive an exception