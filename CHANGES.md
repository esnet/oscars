# OSCARS Release Notes

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
