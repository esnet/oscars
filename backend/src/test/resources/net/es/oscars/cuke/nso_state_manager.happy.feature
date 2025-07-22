@NsoStateManager
@NsoStateManagerHappy
Feature: OSCARS to NSO state manager. This feature pre-validates and queues VPLS and LSP information before synchronization. (Happy path)

  # A VPLS contains a list of SDPs, which is always 2 sdp objects.
  #   Each SDP defines an entry A and an entry B.
  #   Each endpoint has the following properties: device, mode, vc-id, lsp.
  #     The lsp property is a string with the LSP name.

  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. (Add LSP)
    Given I have initialized the world

    Given The NSO state manager loads VPLS and LSP states

    Given The NSO VPLS service state is loaded into the state manager
    Given The NSO VPLS service state has 133 instances in the state manager

    Given The NSO LSP service state is loaded by the state manager
    Given The NSO LSP service state has 536 instances in the state manager

    Then The unmanaged VPLS instances don't exist

    Given The VPLS instance "OSCARS-C2KR" is present in the state manager NSO VPLS service state

    # Add an LSP to a VPLS
    Given The LSP with name "C2KR-PRT-losa-cr6" and device "wash-cr6" from JSON file "http/nso.esnet-lsp.for-oscars-c2kr.json" is added to VPLS "OSCARS-C2KR" as SDP entry "A"
    Given The LSP with name "C2KR-PRT-wash-cr6" and device "losa-cr6" from JSON file "http/nso.esnet-lsp.for-oscars-c2kr.json" is added to VPLS "OSCARS-C2KR" as SDP entry "Z"

    # ... does it validate()?
    When The state manager validates
    Then The state manager is valid
    # ... does it queue()?
#    When The state manager queues
#    Then The state manager local state is queued
    # ... does it sync()?
    When The state manager synchronizes
    Then The state manager is synchronized


  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. (Modify LSP)
    Given I have initialized the world

    Given The NSO state manager loads VPLS and LSP states
    Then The unmanaged VPLS instances don't exist

    Given The NSO VPLS service state is loaded into the state manager
    Given The NSO VPLS service state has 133 instances in the state manager

    Given The NSO LSP service state is loaded by the state manager
    Given The NSO LSP service state has 536 instances in the state manager

    Given The VPLS instance "OSCARS-C2KR" is present in the state manager NSO VPLS service state
    # Modify LSP of a VPLS
    Given I had changed LSP instance in the state manager with name "C7WG-PRT-sunn-cr6" and device "wash-cr6" to name "C7WG-PRT-sunn-cr6" and device "wash-cr6" from "http/nso.esnet-lsp.for-oscars-c7wg.json"
    # ... validate()?
    When The state manager validates
    Then The state manager is valid
    # ... queue()?
#    When The state manager queues
#    Then The state manager local state is queued
    # ... sync()?
    When The state manager synchronizes
    Then The state manager is synchronized



  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. (Delete LSP, not the last LSP)
    Given I have initialized the world

    Given The NSO state manager loads VPLS and LSP states
    Then The unmanaged VPLS instances don't exist

    Given The NSO VPLS service state is loaded into the state manager
    Given The NSO VPLS service state has 133 instances in the state manager

    Given The NSO LSP service state is loaded by the state manager
    Given The NSO LSP service state has 536 instances in the state manager

    Given The VPLS instance "OSCARS-C2KR" is present in the state manager NSO VPLS service state

    # Delete LSP of a VPLS. Last LSP, when deleted, should also cause VPLS to be deleted by default (unless a flag parameter is set to false)
    Given I had deleted LSP instance in the state manager with name "C2WJ-PRT-newy32aoa-cr6" and device "star-cr6"
    Given I had deleted LSP instance in the state manager with name "C2WJ-PRT-star-cr6" and device "newy32aoa-cr6"

    # ... validate()?
    When The state manager validates
    Then The state manager is valid
    # ... queue()?
#    When The state manager queues
#    Then The state manager local state is queued
    # ... sync()?
    When The state manager synchronizes
    Then The state manager is synchronized



  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. Attempt to remove all LSPs from VPLS, VPLS should get deleted too.
    Given I have initialized the world

    Given The NSO state manager loads VPLS and LSP states
    Then The unmanaged VPLS instances don't exist

    Given The NSO VPLS service state is loaded into the state manager
    Given The NSO VPLS service state has 133 instances in the state manager

    Given The NSO LSP service state is loaded by the state manager
    Given The NSO LSP service state has 536 instances in the state manager

    Given The VPLS instance "OSCARS-C2WJ" is present in the state manager NSO VPLS service state

    # Delete LSP of a VPLS. Last LSP, when deleted, should also cause VPLS to be deleted by default (unless a flag parameter is set to false)
    Given I had deleted LSP instance in the state manager with name "C2WJ-PRT-newy32aoa-cr6" and device "star-cr6"
    Given I had deleted LSP instance in the state manager with name "C2WJ-PRT-star-cr6" and device "newy32aoa-cr6"

    Given I had deleted LSP instance in the state manager with name "C2WJ-WRK-newy32aoa-cr6" and device "star-cr6"
    Given I had deleted LSP instance in the state manager with name "C2WJ-WRK-star-cr6" and device "newy32aoa-cr6"

    # ... validate()?
    When The state manager validates
    Then The state manager is valid


    # ... queue()?
#    When The state manager queues
#    Then The state manager local state is queued
    # ... sync()?
    When The state manager synchronizes
    Then The state manager is synchronized

    Then The VPLS "OSCARS-C2WJ" in the state manager was marked "delete"

