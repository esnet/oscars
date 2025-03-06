@NsoLspSyncSteps
Feature: synchronize NSO service state to OSCARS state

  I want to

  Scenario: Read NSO LSP service state, make decisions about add / delete / redeploy
    Given I have initialized the world
    Given I load the NSO LSP service state
    Then The NSO LSP service state has 818 instances
    When I load the list of active OSCARS connections from "connections-active.json"
    Then If I evaluate lsp "AAAA" I will decide that I need to "add"
    Then If I evaluate lsp "BBBB" I will decide that I need to "delete"
    Then If I evaluate lsp "CCCC" I will decide that I need to "redeploy"
    Then If I evaluate lsp "DDDD" I will decide that I need to "no-op"


  Scenario: Read NSO LSP service state, make decisions about multiple add / delete / redeploys
    Given I have initialized the world
    Given I load the NSO LSP service state
    Then The NSO LSP service state has 818 instances
    When I load the list of active OSCARS connections from "connections-active.json"
    Then The list of LSP service instances that need to be "added" equals "add-these.json"
    Then The list of LSP service instances that need to be "deleted" equals "delete-these.json"
    Then The list of LSP service instances that need to be "redeployed" equals "redeploy-these.json"


  Scenario: Modify NSO LSP service state with single adds / deletes / redeploys
    Given I have initialized the world
    Given I load the NSO LSP service state
    Then The NSO LSP service state has 818 instances

    When I add LSP instance "AAAA"
    Then The LSP instance "AAAA" is present in the NSO LSP service state
    Then The NSO LSP service state has 819 instances
    Then The LSP instance "AAAA" is present in the NSO LSP service state
    Then If I evaluate lsp "AAAA" I will decide that I need to "no-op"


    Then The LSP instance "BBBB" is present in the NSO LSP service state
    When I delete LSP instance "BBBB"
    Then The LSP instance "BBBB" is not present in the NSO LSP service state
    Then The NSO LSP service state has 818 instances
    Then If I evaluate lsp "BBBB" I will decide that I need to "no-op"

    Then The LSP instance "CCCC" is present in the NSO LSP service state
    Then The LSP instance "CCCC" does not match "CCCC.json"
    When I redeploy LSP instance "CCCC" to match "CCCC.json"
    Then The LSP instance "CCCC" is present in the NSO LSP service state
    Then The NSO LSP service state has 818 instances
    Then The LSP instance "CCCC" matches "CCCC.json"
    Then If I evaluate lsp "CCCC" I will decide that I need to "no-op"


  Scenario: Modify NSO LSP service state with a big patch that applies one add, one delete, one redeploy all together
    Given I have initialized the world
    Given I load the NSO LSP service state
    Then The NSO LSP service state has 818 instances
    When I apply LSP service patch from "LSP-patch.json"
    Then The NSO LSP service state has 818 instances
    Then The LSP instance "AAAA" is present in the NSO LSP service state
    Then The LSP instance "AAAA" matches "AAAA.json"
    Then The LSP instance "BBBB" is not present in the NSO LSP service state
    Then The LSP instance "CCCC" matches "CCCC.json"
    Then If I evaluate lsp "AAAA" I will decide that I need to "no-op"
    Then If I evaluate lsp "BBBB" I will decide that I need to "no-op"
    Then If I evaluate lsp "CCCC" I will decide that I need to "no-op"


