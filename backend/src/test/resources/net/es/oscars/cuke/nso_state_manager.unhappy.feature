@NsoStateManager
Feature: OSCARS to NSO state manager. This feature pre-validates and queues VPLS and LSP information before synchronization. (Unhappy path)

  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. Attempt to use the same LSP for A and Z.

  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. Attempt to use an LSP already in use elsewhere.

  Scenario: One (1) VPLS, associated with one (1) LSP at point A, and one (1) LSP at point B. Attempt to use a non-existent LSP.

  Scenario: One (1) VPLS, associated with multiple LSPs.

  Scenario: Two (2) VPLS, associated with multiple LSPs.