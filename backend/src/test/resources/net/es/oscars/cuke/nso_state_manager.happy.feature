@NsoStateManager
Feature: OSCARS to NSO state manager. This feature pre-validates and queues VPLS and LSP information before synchronization. (Happy path)

  # A VPLS contains a list of SDPs, which is always 2 sdp objects.
  #   Each SDP defines an entry A and an entry B.
  #   Each endpoint has the following properties: device, mode, vc-id, lsp.
  #     The lsp property is a string with the LSP name.

  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. (Add LSP)
    Given I have initialized the world

    Given The NSO state manager loads VPLS and LSP states

    Given The NSO VPLS service state is loaded into the state manager
    Given The NSO VPLS service state has 137 instances in the state manager

    Given The NSO LSP service state is loaded by the state manager
    Given The NSO LSP service state has 536 instances in the state manager

    Given The VPLS instance "OSCARS-C2KR" is present in the state manager NSO VPLS service state

    # Add an LSP to a VPLS
    Given The LSP with name "C2KR-PRT-losa-cr6" and device "wash-cr6" from JSON file "http/nso.esnet-lsp.for-oscars-c2kr.json" is added to VPLS "OSCARS-C2KR" as SDP entry "A"

    # ... does it validate()?
    When The state manager validates
    Then The state manager is valid
    # ... does it queue()?
#    When The state manager queues
#    Then The state manager local state is queued
    # ... does it sync()?
    When The state manager synchronizes
    Then The state manager is synchronized


#  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. (Modify LSP)
#    Given I have initialized the world
#
#    Given The NSO state manager loads VPLS and LSP states
#
#    Given The NSO VPLS service state is loaded into the state manager
#    Given The NSO VPLS service state has 137 instances in the state manager
#
#    Given The NSO LSP service state is loaded by the state manager
#    Given The NSO LSP service state has 536 instances in the state manager
#
#    Given The VPLS instance "OSCARS-C2KR" is present in the state manager NSO VPLS service state
    # Modify LSP of a VPLS
    # ... validate()?
    # ... queue()?
    # ... sync()?

    # Delete LSP of a VPLS
    # ... validate()
    # ... queue()
    # ... sync()

  Scenario: One (1) VPLS, associated with multiple AZ listings.
    # Add an LSP to a VPLS
    # ... validate()
    # ... queue()
    # ... sync()

    # Modify LSP of a VPLS
    # ... validate()
    # ... queue()
    # ... sync()

    # Delete LSP of a VPLS
    # ... validate()
    # ... queue()
    # ... sync()

  Scenario: Two (2) VPLS, associated with multiple AZ listings.
    # Add an LSP to a VPLS
    # ... validate()
    # ... queue()
    # ... sync()

    # Modify LSP of a VPLS
    # ... validate()
    # ... queue()
    # ... sync()

    # Delete LSP of a VPLS
    # ... validate()
    # ... queue()
    # ... sync()