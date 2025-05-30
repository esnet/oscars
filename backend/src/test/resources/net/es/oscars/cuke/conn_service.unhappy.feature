@NsoProxySteps
Feature: Make OSCARS OP Commands netconf ned ready

  I want to verify that OSCARS stats work regardless if the device is on ALU CLI or Nokia NC NED.

  Scenario: Read the NSO Proxy live-status endpoint (happy path)
    # Mps reservation is only 1000 Mbps... attempt to reserve 1001 Mbps. Should fail.