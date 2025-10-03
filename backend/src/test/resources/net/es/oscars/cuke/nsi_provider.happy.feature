@NsiProviderSteps
@NsiProviderStepsHappy
Feature: The NSI SOAP API provider endpoints (Happy)
    Scenario: An NSI connection is reserved without a projectId field
        Given The NSI connection is queued for asynchronous reservation while not including a projectId
        Given The NSI queue size is 1
        When The NSI queue is processed
        Then The NSI connection is put on hold
        And The NSI connection does not have a projectId
        And The NSI provider encountered 0 errors

   Scenario: An NSI connection is reserved with a projectId field
       Given The NSI connection is queued for asynchronous reservation while including a projectId
       Given The NSI queue size is 1
       When The NSI queue is processed
       Then The NSI connection is put on hold
       And The NSI connection has a projectId
       And The NSI provider encountered 0 errors

    Scenario: An NSI connection is reserved with a blank projectId field
       Given The NSI connection is queued for asynchronous reservation while including a blank projectId
       Given The NSI queue size is 1
       When The NSI queue is processed
       Then The NSI connection is put on hold
       And The NSI connection does not have a projectId
       And The NSI provider encountered 0 errors

    Scenario: NSI Reserve test suite - not reserved yet

        # Draft NSI CS protocol 2.1 v13 at https://redmine.ogf.org/attachments/286/draft-nsi-cs-protocol-2dot1-v13.pdf
        # See  https://gitlab.es.net/esnet/nsi-soap/-/blob/main/src/main/java/net/es/nsi/lib/soap/gen/nsi_2_0/connection/types/ReservationStateEnumType.java?ref_type=heads


        # NSI reserve() hold
        Given The connection is not reserved yet
        When An NSI connection reserve is requested
        Then The latest connection event type is "RESERVE_RECEIVED"
        Then The NSI queue is processed
        Then The reservation state path was "RESERVE_CHECKING -> RESERVE_HELD"

        # NSI commit()
        # Given The reservation state is now "RESERVE_HELD"
        # When An NSI connection commit is requested
        # Then The NSI queue is processed
        # Then The reservation state path was "RESERVE_CHECKING -> RESERVE_HELD -> RESERVE_COMMITTING -> RESERVE_START"



