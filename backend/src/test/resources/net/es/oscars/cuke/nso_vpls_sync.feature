@NsoVplsSyncSteps
Feature: synchronize NSO service state to OSCARS state

  I want to

  Scenario: Read NSO VPLS service state, make decisions about add / delete / redeploy
    Given I have initialized the world
    Given I load the NSO VPLS service state
    Then The NSO VPLS service state has 139 instances
    When I load the list of active OSCARS connections from "connections-active.json"

    # all the various evaluation functions live in NsoStateSyncer
    Then If I evaluate vpls "AAAA" I will decide that I need to "add"
    Then If I evaluate vpls "BBBB" I will decide that I need to "delete"
    Then If I evaluate vpls "CCCC" I will decide that I need to "redeploy"
    Then If I evaluate vpls "DDDD" I will decide that I need to "no-op"


  Scenario: Read NSO VPLS service state, make decisions about multiple add / delete / redeploys
    Given I have initialized the world
    Given I load the NSO VPLS service state
    Then The NSO VPLS service state has 139 instances
    When I load the list of active OSCARS connections from "connections-active.json"
    Then The list of VPLS service instances that need to be "added" equals "add-these.json"
    Then The list of VPLS service instances that need to be "deleted" equals "delete-these.json"
    Then The list of VPLS service instances that need to be "redeployed" equals "redeploy-these.json"


  Scenario: Modify NSO VPLS service state with single adds / deletes / redeploys
    Given I have initialized the world
    Given I load the NSO VPLS service state
    Then The NSO VPLS service state has 139 instances

    When I add VPLS instance "AAAA"
    Then The VPLS instance "AAAA" is present in the NSO VPLS service state
    Then The NSO VPLS service state has 140 instances
    Then The VPLS instance "AAAA" is present in the NSO VPLS service state
    Then If I evaluate vpls "AAAA" I will decide that I need to "no-op"


    Then The VPLS instance "BBBB" is present in the NSO VPLS service state
    When I delete VPLS instance "BBBB"
    Then The VPLS instance "BBBB" is not present in the NSO VPLS service state
    Then The NSO VPLS service state has 139 instances
    Then If I evaluate vpls "BBBB" I will decide that I need to "no-op"

    Then The VPLS instance "CCCC" is present in the NSO VPLS service state
    Then The VPLS instance "CCCC" does not match "CCCC.json"
    When I redeploy VPLS instance "CCCC" to match "CCCC.json"
    Then The VPLS instance "CCCC" is present in the NSO VPLS service state
    Then The NSO VPLS service state has 139 instances
    Then The VPLS instance "CCCC" matches "CCCC.json"
    Then If I evaluate vpls "CCCC" I will decide that I need to "no-op"


  Scenario: Modify NSO VPLS service state with a big patch that applies one add, one delete, one redeploy all together
    Given I have initialized the world
    Given I load the NSO VPLS service state
    Then The NSO VPLS service state has 139 instances
    When I apply VPLS service patch from "vpls-patch.json"
    Then The NSO VPLS service state has 139 instances
    Then The VPLS instance "AAAA" is present in the NSO VPLS service state
    Then The VPLS instance "AAAA" matches "AAAA.json"
    Then The VPLS instance "BBBB" is not present in the NSO VPLS service state
    Then The VPLS instance "CCCC" matches "CCCC.json"
    Then If I evaluate vpls "AAAA" I will decide that I need to "no-op"
    Then If I evaluate vpls "BBBB" I will decide that I need to "no-op"
    Then If I evaluate vpls "CCCC" I will decide that I need to "no-op"


