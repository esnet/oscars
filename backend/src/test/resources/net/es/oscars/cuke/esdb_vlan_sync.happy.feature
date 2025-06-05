@EsdbVlanSyncSteps
@EsdbVlanSyncStepsHappy
Feature: Process ESDB VLAN synchronization. (Happy Path)

  Scenario: An ESDB VLAN synchronization is triggered (manual).

    Given The ESDB VLAN task is ready
    When The ESDB VLAN synchronization is triggered
    Then The ESDB VLAN was synchronized
