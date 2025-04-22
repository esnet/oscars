@NsoLspSyncSteps
Feature: synchronize NSO service state to OSCARS state, LSP

  I want to verify that NSO service state is synchronized to the OSCARS state (Happy Path).
  Evaluation mechanism should automatically mark LSPs as one of "add", "delete", "redeploy", or "no-op".

  Evaluate -> Mark -> Synchronize.

  Scenario: Read NSO's LSP service state, make decisions about add / delete / redeploy
    Given I have initialized the world
    Given I have retrieved the NSO LSPs
    Given The NSO LSP service state is loaded
    Given The NSO LSP service state has 137 instances

    # AAAA (add VPLS, add LSPs)

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
