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

  Scenario: Read NSO's LSP service state, make decisions about add / delete / redeploy
    Given I have initialized the world
    Given I have retrieved the NSO LSPs
    Given The NSO LSP service state is loaded
    Given The NSO LSP service state has 536 instances

    # AAAA (add VPLS, add LSPs)
    # ... VPLS "OSCARS-C2KR" as endpoint "A"
    Given I had added LSP instance name "C2KR-WRK-losa-cr6" with device "wash-cr6" from "http/nso.esnet-lsp.for-oscars-c2kr.json"
    # ... VPLS "OSCARS-C2KR" as endpoint "Z"
    Given I had added LSP instance name "C2KR-WRK-wash-cr6" with device "losa-cr6" from "http/nso.esnet-lsp.for-oscars-c2kr.json"

    When I perform an LSP synchronization
    Then The NSO LSP service is synchronized
    Then The NSO LSP service state has 538 instances


    # AAAA (add VPLS, redeploy existing LSPs)

    # AAAA (add VPLS, delete existing LSPs)

    # AAAA (add VPLS, no-op LSPs)


    # BBBB (delete VPLS, delete LSPs)

    # BBBB (delete VPLS, no-op LSPs)


    # CCCC (redeploy VPLS, add LSPs)

    # CCCC (redeploy VPLS, redeploy LSPs)

    # CCCC (redeploy VPLS, delete LSPs)

    # CCCC (redeploy VPLS, no-op LSPs)


    # DDDD (no-op VPLS, add LSPs)

    # DDDD (no-op VPLS, delete LSPs)

    # DDDD (no-op VPLS, redeploy LSPs)

    # DDDD (no-op VPLS, no-op LSPs)
