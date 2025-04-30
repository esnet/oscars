@NsoLspSyncSteps
Feature: synchronize NSO service state to OSCARS state, LSP

  I want to verify that NSO service state is synchronized to the OSCARS state (Happy Path).
  Evaluation mechanism should automatically mark LSPs as one of "add", "delete", "redeploy", or "no-op".

  Evaluate -> Mark -> Synchronize.

  # Rules for LSPs. (@haniotak)
  #
  # - An LSP is associated with exactly one VPLS ; a VPLS will have multiple LSPs
  # - No LSPs should be present in NSO that are not associated with a VPLS ("orphans")
  #
  # DELETE
  # - When a VPLS is deleted, all LSPs associated with it should be deleted
  # - To delete an LSP associated with a VPLS, the VPLS must be deleted first
  # - You can always delete an "orphan" LSP if you find one
  #
  # ADD
  # - You must add all LSPs before adding their associated VPLS
  # - Do not add any "orphan" LSPs
  #
  # REDEPLOY
  # - Redeploying a VPLS can mean it is associated with a new set of VPLSs  - i.e. from {A, B, C} to {B, C', D} (C' has changed and needs to be redeployed)
  # - Add any new LSPs first (D), redeploy any LSPs that changed (C), redeploy the VPLS itself, then delete the newly orphan LSPs (A)

  Scenario: Read NSO's LSP service state, ADD evaluation.
    Given I have initialized the world
    Given I have retrieved the NSO LSPs
    Given The NSO LSP service state is loaded
    Given The NSO LSP service state has 536 instances

    # Add LSPs
    Given I had added LSP instance name "C2KR-WRK-losa-cr6" with device "wash-cr6" from "http/nso.esnet-lsp.for-oscars-c2kr.json"

    When I evaluate LSP with name "C2KR-WRK-losa-cr6" and device "wash-cr6"
    Then The list of LSP service instances marked "add" has a count of 1

  Scenario: Read NSO's LSP service state, REDEPLOY evaluation.
    Given I have initialized the world
    Given I have retrieved the NSO LSPs
    Given The NSO LSP service state is loaded
    Given The NSO LSP service state has 536 instances

    # Redeploy LSPs
    Given I had changed LSP instance with name "C7WG-PRT-sunn-cr6" and device "wash-cr6" to name "C7WG-PRT-sunn-cr6" and device "wash-cr6" from "http/nso.esnet-lsp.for-oscars-c7wg.json"

    When I evaluate LSP with name "C7WG-PRT-sunn-cr6" and device "wash-cr6"
    Then The list of LSP service instances marked "redeploy" has a count of 1

  Scenario: Read NSO's LSP service state, ADD synchronization.
    Given I have initialized the world
    Given I have retrieved the NSO LSPs
    Given The NSO LSP service state is loaded
    Given The NSO LSP service state has 536 instances

    # Add LSPs
    # ... Note, VPLS "OSCARS-C2KR" as endpoint "A"
    Given I had added LSP instance name "C2KR-WRK-losa-cr6" with device "wash-cr6" from "http/nso.esnet-lsp.for-oscars-c2kr.json"
    # ... Note, VPLS "OSCARS-C2KR" as endpoint "Z"
    Given I had added LSP instance name "C2KR-WRK-wash-cr6" with device "losa-cr6" from "http/nso.esnet-lsp.for-oscars-c2kr.json"

    When I perform an LSP synchronization

    Then The list of LSP service instances marked "add" has a count of 2
    Then The NSO LSP service is synchronized
    Then The NSO LSP service state has 538 instances


    # Redeploy LSPs
  Scenario: Read NSO's LSP service state, REDEPLOY synchronization.
    Given I have initialized the world
    Given I have retrieved the NSO LSPs
    Given The NSO LSP service state is loaded
    Given The NSO LSP service state has 536 instances

    Given I had marked LSP instance with name "C2WJ-PRT-newy32aoa-cr6" and device "star-cr6" as "redeploy"

    When I perform an LSP synchronization

    Then The list of LSP service instances marked "redeploy" has a count of 1
    Then The NSO LSP service is synchronized

    # Delete LSPs
  Scenario: Read NSO's LSP service state, DELETE synchronization.
    Given I have initialized the world
    Given I have retrieved the NSO LSPs
    Given The NSO LSP service state is loaded
    Given The NSO LSP service state has 536 instances
    Given I had marked LSP instance with name "C2WJ-PRT-newy32aoa-cr6" and device "star-cr6" as "delete"

    When I perform an LSP synchronization

    Then The list of LSP service instances marked "delete" has a count of 1
    Then The NSO LSP service is synchronized

    # NO-OP LSPs
  Scenario: Read NSO's LSP service state, NOOP check.
    Given I have initialized the world
    Given I have retrieved the NSO LSPs
    Given The NSO LSP service state is loaded
    Given The NSO LSP service state has 536 instances

    # Add LSPs
    # ... Note, VPLS "OSCARS-C2KR" as endpoint "A"
    Given I had added LSP instance name "C2KR-WRK-losa-cr6" with device "wash-cr6" from "http/nso.esnet-lsp.for-oscars-c2kr.json"
    # ... Note, VPLS "OSCARS-C2KR" as endpoint "Z"
    Given I had added LSP instance name "C2KR-WRK-wash-cr6" with device "losa-cr6" from "http/nso.esnet-lsp.for-oscars-c2kr.json"

    When I perform an LSP synchronization

    Then The list of LSP service instances marked "add" has a count of 2
    Then The list of LSP service instances marked "no-op" has a count of 536
    Then The NSO LSP service is synchronized
    Then The NSO LSP service state has 538 instances
