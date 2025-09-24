@NsiProviderSteps
@NsiProviderStepsHappy
Feature: The NSI SOAP API provider endpoints (Happy)
    Scenario: An NSI connection is reserved without a projectId field
        Given The NSI connection is queued for asynchronous reservation while not including a projectId
        Given The NSI queue size is 1
        When The NSI reserve is requested
        Then The NSI connection is put on hold
        And The NSI connection does not have a projectId
        And The NSI provider encountered 0 errors

   Scenario: An NSI connection is reserved with a projectId field
       Given The NSI connection is queued for asynchronous reservation while including a projectId
       Given The NSI queue size is 1
       When The NSI reserve is requested
       Then The NSI connection is put on hold
       And The NSI connection has a projectId
       And The NSI provider encountered 0 errors

    Scenario: An NSI connection is reserved with a blank projectId field
       Given The NSI connection is queued for asynchronous reservation while including a blank projectId
       Given The NSI queue size is 1
       When The NSI reserve is requested
       Then The NSI connection is put on hold
       And The NSI connection does not have a projectId
       And The NSI provider encountered 0 errors

    Scenario: NSI Reserve test suite - not reserved yet

        Given The connection is not reserved yet
        When An NSI connection reserve is requested
        Then The NSI mapping and connection object is created
        # And The reservation state is now "reserve checking"
        And The reservation state is now "RESERVE_RECEIVED"

        
        # Given The connection reservation state is "reserve checking"
        Given The connection reservation state is "RESERVE_RECEIVED"
        When The NSI reserve is requested
        When The resources ARE available
        # Then The reservation state is now "reserve held"
        Then The reservation state is now "RESERVE_CONFIRM"
        And The resources are no longer available for something else
        And The reserveConfirmed message callback is triggered

        # Given The connection reservation state is "reserve checking"
        # When The resources ARE NOT available
        # #Then The reservation state is now "reserve failed"
        # Then The reservation state is now "RESERVE_FAILED"
        # And The resources are available for something else
        # And The reserveFailed message callback is triggered
