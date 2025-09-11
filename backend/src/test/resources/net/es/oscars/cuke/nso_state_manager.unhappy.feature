@NsoStateManager
@NsoStateManagerUnhappy
Feature: OSCARS to NSO state manager. This feature pre-validates and queues VPLS and LSP information before synchronization. (Unhappy path)

  Scenario: One (1) VPLS, where each SDP is associated with one (1) LSP at point A, and one (1) LSP at point B. Attempt to use the same LSP for A and Z.
    Given I have initialized the world
    Given The NSO state manager loads VPLS and LSP states

    Given The NSO VPLS service state is loaded into the state manager
    Given The NSO VPLS service state has 133 instances in the state manager

    Given The NSO LSP service state is loaded by the state manager
    Given The NSO LSP service state has 536 instances in the state manager

    Then The unmanaged VPLS instances don't exist

    Given The VPLS instance "OSCARS-C2KR" is present in the state manager NSO VPLS service state

    # Add an LSP to a VPLS
    When The LSP with name "C2KR-PRT-losa-cr6" and device "wash-cr6" from JSON file "http/nso/lsp/nso.esnet-lsp.for-oscars-c2kr.json" is added
    When The LSP with name "C2KR-PRT-wash-cr6" and device "losa-cr6" from JSON file "http/nso/lsp/nso.esnet-lsp.for-oscars-c2kr.json" is added
    When The LSP with name "C2KR-WRK-losa-cr6" and device "wash-cr6" from JSON file "http/nso/lsp/nso.esnet-lsp.for-oscars-c2kr.json" is added
    When The LSP with name "C2KR-WRK-wash-cr6" and device "losa-cr6" from JSON file "http/nso/lsp/nso.esnet-lsp.for-oscars-c2kr.json" is added

    When The VPLS instance "OSCARS-C2KR" from JSON file "http/nso/vpls/nso.esnet-vpls.for-oscars-c2kr.same-az-lsp.json" is put in the state manager
    When The state manager validates
    Then The state manager is not valid

  Scenario: One (1) VPLS, where each SDP is associated with one (1) LSP at point A, and one (1) LSP at point B. We added an uneven number of LSPs for the VPLS.
    Given I have initialized the world
    Given The NSO state manager loads VPLS and LSP states
    Then The unmanaged VPLS instances don't exist

    Given The NSO VPLS service state is loaded into the state manager
    Given The NSO VPLS service state has 133 instances in the state manager

    Given The NSO LSP service state is loaded by the state manager
    Given The NSO LSP service state has 536 instances in the state manager

    Given The VPLS instance "OSCARS-C2KR" is present in the state manager NSO VPLS service state

    # Add an LSP to a VPLS
    When The LSP with name "C2KR-PRT-losa-cr6" and device "wash-cr6" from JSON file "http/nso/lsp/nso.esnet-lsp.for-oscars-c2kr.json" is added
    When The LSP with name "C2KR-PRT-wash-cr6" and device "losa-cr6" from JSON file "http/nso/lsp/nso.esnet-lsp.for-oscars-c2kr.json" is added
    When The LSP with name "C2KR-WRK-losa-cr6" and device "wash-cr6" from JSON file "http/nso/lsp/nso.esnet-lsp.for-oscars-c2kr.json" is added

    When The state manager validates
    Then The state manager is not valid
