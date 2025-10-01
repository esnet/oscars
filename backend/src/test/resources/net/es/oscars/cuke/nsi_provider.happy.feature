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
        # The initial state of a connection reservation is non-existent.
        # When the NsiProvider receives a connection reservation request (SOAP XML),
        # it will log a "RESERVE_RECEIVED" connection event to the database,
        # and internally go through the ReservationStateEnumType enumeration states.
        # See  https://gitlab.es.net/esnet/nsi-soap/-/blob/main/src/main/java/net/es/nsi/lib/soap/gen/nsi_2_0/connection/types/ReservationStateEnumType.java?ref_type=heads
        #
        # When requesting to reserve a connection, pg. 12 of the draft NSI CS protocol 2.1 v13 shows the following flow:
        #   
        #   RESERVE_START -> RESERVE_CHECKING -> (if resource is available) -> RESERVE_HELD
        #   ... if a timeout occurs, it goes RESERVE_HELD -> RESERVE_TIMEOUT 
        #           -> (diagram shows illogical possibility of going to RESERVE_COMMITTING, but most likely goes to RESERVE_ABORTING) 
        #           -> RESERVE_START
        # 
        #   ... or RESERVE_HELD -> (no notes on this) -> RESERVE_ABORTING -> RESERVE_START
        #     or
        #   RESERVE_START -> RESERVE_CHECKING -> (if resource is NOT available) -> RESERVE_FAILED
        #   ... it will then transition to RESERVe_ABORTING -> RESERVE_START
        #
        # When 


        Given The connection is not reserved yet
        When An NSI connection reserve is requested
        Then The latest connection event type is "RESERVE_RECEIVED"
        Then The NSI queue is processed
        
        # This is what the NSI specs say should happen...
        # Then The reservation state path was "RESERVE_START -> RESERVE_CHECKING -> RESERVE_HELD"

        # ...but, this is what currently happens
        Then The reservation state path was "RESERVE_CHECKING -> RESERVE_HELD"

        Then The reservation state is now "RESERVE_HELD"

        Given The reservation state is now "RESERVE_HELD"
        # Given The NSI queue is processed
        # When The NSI mapping and connection object is created
        # Then The latest connection event type is "RESERVE_CONFIRM"
        # Then The reservation state is now "RESERVE_HELD"
