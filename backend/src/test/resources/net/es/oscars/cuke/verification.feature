@unit
Feature: design verification

  I want to make sure that I can verify designs against baseline topology

  Scenario: Parameters verification
    Given I have initialized the world
    Given I clear the topology
    Given I load topology from "topo/one.json"
    When I merge the new topology
    Given I update the topology URN map after import
    Given I load a design from "designs/one_router.json"
    Then I "can" verify the design against baseline
    Given I load a design from "designs/invalid_missing_junction_urn.json"
    Then I "can not" verify the design against baseline
    Given I load a design from "designs/invalid_wrong_type_junction_urn.json"
    Then I "can not" verify the design against baseline
    Given I load a design from "designs/invalid_missing_fixture_urn.json"
    Then I "can not" verify the design against baseline
    Given I load a design from "designs/invalid_wrong_type_fixture_urn.json"
    Then I "can not" verify the design against baseline
    Given I load a design from "designs/invalid_insufficient_fixture_ingress.json"
    Then I "can not" verify the design against baseline
    Given I load a design from "designs/invalid_insufficient_fixture_egress.json"
    Then I "can not" verify the design against baseline
    Given I load a design from "designs/invalid_nonreservable_fixture_vlan.json"
    Then I "can not" verify the design against baseline
    Then I did not receive an exception