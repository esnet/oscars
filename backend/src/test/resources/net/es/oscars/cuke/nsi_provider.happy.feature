@NsiProviderSteps
@NsiProviderStepsHappy
Feature: The NSI SOAP API provider endpoints (Happy)
    Scenario: An NSI connection is reserved without a projectId field
        Given The NSI connection is queued for asynchronous reservation while not including a projectId
        Given The NSI queue size is 1
        When The NSI reserve is requested
#        Then The NSI connection is put on hold  # commented out cos it's not implemented yet
        And The NSI connection does not have a projectId
        And The NSI provider encountered 0 errors
#    Scenario: An NSI connection is reserved with a projectId field
#        Given The NSI connection is queued for asynchronous reservation while including a projectId
#        Given The NSI queue size is 1
#        When The NSI reserve is requested
##        Then The NSI connection is put on hold
#        And The NSI connection has a projectId
#        And The NSI provider encountered 0 errors
