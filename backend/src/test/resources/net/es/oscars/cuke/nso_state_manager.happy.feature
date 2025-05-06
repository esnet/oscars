@NsoStateManager
Feature: OSCARS to NSO state manager. This feature pre-validates and queues VPLS and LSP information before synchronization. (Happy path)

  # A VPLS contains a list of SDPs, which is always 2 sdp objects.
  #   Each SDP defines an entry A and an entry B.
  #   Each endpoint has the following properties: device, mode, vc-id, lsp.
  #     The lsp property is a string with the LSP name.

  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B.
    Given I have initialized the world

    Given The list of active OSCARS connections are loaded
    Given The NSO VPLS service state is loaded
    Given The NSO VPLS service state has 137 instances

    Given I have retrieved the NSO LSPs
    Given The NSO LSP service state is loaded
    Given The NSO LSP service state has 536 instances

    Given The VPLS instance "OSCARS-C2KR" is present in the NSO VPLS service state

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