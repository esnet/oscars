# OSCARS Release Notes
### 1.2.33
> Sep 2025
- OS-634 NSO operation reordering
- OS-617 defensive programming against topo NPE 
- OS-618 try to stop sending null values to NSO
- OS-590 de-mockify EseApiController tests
- OS-640 multiple project ids per connection

### 1.2.32
> Sep 2025
- OS-600 CORS origin customization
- OS-604 YANG PATCH serialization hotfix
- OS-579 Project id frontend 
 
### 1.2.30
> July 2025
- ESE integration
- NSI test suite

### 1.2.29
> June 2025
- OS-522 fuzzy connection search
- OS-529 SENSE query improvements & event REST API
- OS-489 GraphQL queries to ESDB

### 1.2.28
> June 2025
- OS-499 NSI queryNotification() implementation
- OS-495 holdConnection() and validation refactor
- OS-505 NSI syncQuery() should not return p2p criteria before initial commit()
- OS-510 improve NSI syncQuery()
- OS-516 speed up commit()

### 1.2.27
> May 2025
- OS-496 logevent error

### 1.2.26
> May 2025
- NSI fixes
- NSO VPLS / LSP state manager


### 1.2.25
> May 2025
- NSI rewrite
- HELD reservations no longer written to DB, hold -> commit flow updated
- adding a few untagged ports to standalone topology
- add encapsulation to EdgePort
- remove jackson customization, use .properties config instead
- pom.xml cleanups, dependency updates

### 1.2.24
> Apr 2025
- OS-442: Implement NsoLspStateSyncer class to manage NSO LSP state synchronization between OSCARS and NSO.
- SouthboundPeriodicSyncer Scheduled Task implemented to periodically synchronize between OSCARS and NSO state.
- NsoSyncController REST API endpoint at `/protected/nso-sync` added to provide REST API endpoint functionality to the OSCARS to NSO state synchronization mechanisms.

### 1.2.23
> Mar 2025
- OS-421: Implement NsoVplsStateSyncer class to manage NSO VPLS state synchronization between OSCARS and NSO.
- NSI fix for disabled callbacks

### 1.2.22
> Mar 2025
- OS-391: NsoProxy.getLiveStatusShow() refactored to call NSO service endpoint at `restconf/data/esnet-status:esnet-status/nokia-show` with JSON payload string. Bug fix to handle HTTP POST response that may be LiveStatusOutput or IetfRestconfErrorResponse. 
- OS-391: Added happy and unhappy path tests for NsoProxy.getLiveStatusShow().
- 
### 1.2.21
> Feb 2025
- OS-417: misc hotfixes
- re-added test infrastructure

### 1.2.20
> Feb 2025


### 1.2.19
> Jan 2025
- Support for 'untagged' (`EthernetEncapsulation.NULL`) and QINQ (`EthernetEncapsulation.QINQ`) ethernet encapsulation labeling. Application properties `features.untagged-ports` and `features.qinq-ports` flags added.

### 1.2.18
> Jan 2025
- NSI response size hotfix

### 1.2.15
> Oct 2024
- NSI modify hotfix
- remove RANCID code


### 1.2.14
> Oct 2024
- NSI modify fix
- NSI list memory leak fix

### 1.2.13
> Jul 2024
- Topology hotfix

### 1.2.12
> Jul 2024
- Add LSP waypoint search API
 
### 1.2.10
> May 2024
- Add port utilization report API

### 1.2.8
> May 2024
- Add edge port search
- Default reservation time now 10 years
- Minor bugfixes & enhancements

### 1.2.7
> May 2024
- Swagger update

### 1.2.6
> Apr 2024
- OTEL updates

### 1.2.5
> Apr 2024
- prepend "OSCARS" to VPLS service name
- standalone setup for ESE development
- bug fix for ERO ordering 

### 1.2.4
> Mar 2024
- further opentelemetry integration
- bug fixes for Hibernate commit errors

### 1.2.3
> Mar 2024
- OS-285: fix operational state bugs

### 1.2.2
> Mar 2024
- OS-227: add an ip interface frontend
- OS-285: fix operational state bugs
- OS-288: add opentelemetry jars
- OS-286: add raw text for operational status
- update Spring boot version
- remove loading topology from JSON files


### 1.2.1
> Mar 2024
- OS-264: commit labels for OSCARS NSO commits 
- OS-249: operational state backend
- OS-272: operational state frontend

### 1.2.0
> Feb 2024
- added in-service modification feature

### 1.1.6
> Feb 2024
- fixed MAC address collection

### 1.1.5
> Jan 2024
- Removed topology persistence
- add nsi.resv-timeout

### 1.1.4
>  Nov 2023
- [OS-146](https://esnet.atlassian.net/browse/OS-146) Modify OSCARS provisioning to add description to LSP name.
- [OS-147](https://esnet.atlassian.net/browse/OS-147) Add cflowd to OSCARS circuit provisioning
- [OS-242](https://esnet.atlassian.net/browse/OS-242) Externalize NSI class files


## 1.0.45
> Apr, 2023
- Maintenance release
- NSI fixes
- Template updates

## 1.0.43
> Oct 28, 2020
- Maintenance release
- NSI fixes
- Multiple library upgrades

## 1.0.42
> Aug 12, 2020
- Maintenance release
- Frontend:
    - Library updates & other misc maintenance
- Backend
    - Template changes

## v1.0.41
> Mar 9, 2020
- Frontend:
    - Add clone feature
    - Build / dismantle buttons fixes
    - Library updates & other misc maintenance
- Backend
    - Add clone validation API
    - Integrate logging / syslog config 


## v1.0.39
> Oct 22, 2019
- Frontend:
    - Display config template version 
    - Bugfixes

## v1.0.38 
> Oct 18, 2019
- Frontend:
    - Bugfixes

## v1.0.37
> Oct 14, 2019
- Backend:
    - Versioned templates feature
    - Juniper fixes
    - Logging to remote syslogd
    
- Frontend:
    - Cosmetic fixes
    - Security vulnerability updates


## v1.0.36
> Aug 6, 2019
- Backend:
    - PSS bug hotfix

- Frontend:
    - Minor cosmetic fixes
    
## v1.0.35
> July 30, 2019
- Backend:
    - PSS config generation refactoring 
    - PSS improved queueing, bug fixing
    - Add a opStatus REST endpoint
    - Allow partial match for map positions
    - Add a PSS work status API
- Frontend:
    - Add PSS Feedback on the connection description page
    - Add the option for multivalue text input 

## v1.0.34
> May 21, 2019
- Frontend:
    - Fix connection list slow fetch times   
    - Restore prior default phase option "Reserved"
- Backend:
    - No changes
    
## v1.0.33
> May 15, 2019
- Backend:
    - Extensive topology changes
    - Minor PSS bug fixes
    - Improved validation
    - Tag categories
- Frontend:
    - ASAP schedule option, is the new default
    - List page improvements
    - Tag controls in New Connection
    - Library updates & other misc maintenance
- Improved versioning with Maven

## v1.0.32
> Apr 16, 2019
- Updated Spring Boot to v2
- PCE performance improvements


## v1.0.30

> Mar 12, 2019

- Moved repository to `esnet/oscars`
- Merged frontend code
- Allow users to modify end time and description of a connection [#304](https://github.com/esnet/oscars/issues/304),[#300](https://github.com/esnet/oscars/issues/300),[#287](https://github.com/esnet/oscars/issues/287)
- Hotfix for new API endpoint issues
- Add SDP Information

## v1.0.27

> Mar 6, 2019

- Fixes in MX LSPs templates (priority and metrics)
- Topology python script fixes for incorrect loopbacks
- Add API for historical data [#285](https://github.com/esnet/oscars/issues/285)
- Give a better name to the juniper OSCARS community [#283](https://github.com/esnet/oscars/issues/283)
- Parameterize minimum connection duration [#271](https://github.com/esnet/oscars/issues/271)
- Conditionally set output-vlan-map swap  [#282](https://github.com/esnet/oscars/issues/282)
- Adds NSI forced-end message 
- Fix for bad loopbacks [#284](https://github.com/esnet/oscars/issues/284)
- Fix [#281](https://github.com/esnet/oscars/issues/281)

## v1.0.26

> Feb 21, 2019

- Fix latitude / longitude inversion in config file nsi.nsa-location [#273](https://github.com/esnet/oscars/issues/273)
- Deparallelize NSI callbacks [#272](https://github.com/esnet/oscars/issues/272)
- Handle if-modified-since header in requests [#238](https://github.com/esnet/oscars/issues/238)
- Add feature to allow service MTU override [#258](https://github.com/esnet/oscars/issues/258)
- Fix NSI version handling [#266](https://github.com/esnet/oscars/issues/266)
- Make port/device geo locations available thru API (/api/topo/locations) [#275](https://github.com/esnet/oscars/issues/275)
- Update frontend to 1.0.21

## v1.0.25

> Feb 7, 2019

- Add physical port locations to NML topology [#264](https://github.com/esnet/oscars/issues/264)
- Fix a topology loopback address bug
- Fix for source / destination flip
- Correct filter behavior for empty phase
- Update frontend to 1.0.20
