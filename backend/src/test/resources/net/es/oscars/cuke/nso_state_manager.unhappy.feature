@NsoStateManager
@NsoStateManagerUnhappy
Feature: OSCARS to NSO state manager. This feature pre-validates and queues VPLS and LSP information before synchronization. (Unhappy path)

  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. Attempt to use the same LSP for A and Z.
    Given I have initialized the world
    Given The world is expecting an exception
    Given The NSO state manager loads VPLS and LSP states

    Given The NSO VPLS service state is loaded into the state manager
    Given The NSO VPLS service state has 137 instances in the state manager

    Given The NSO LSP service state is loaded by the state manager
    Given The NSO LSP service state has 536 instances in the state manager

    Given The VPLS instance "OSCARS-C2KR" is present in the state manager NSO VPLS service state

    When The VPLS instance "OSCARS-C2KR" from JSON file "http/nso.esnet-vpls.for-oscars-c2kr.same-az-lsp.json" is put in the state manager

    Then I did receive an exception

  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. Attempt to use an LSP already in use elsewhere.

  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. Attempt to use a non-existent LSP.

  Scenario: One (1) VPLS, associated with multiple LSPs.

  Scenario: Two (2) VPLS, associated with multiple LSPs.