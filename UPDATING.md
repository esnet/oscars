# Updating OSCARS
This file contains instructions for updating an existing installation of OSCARS from a previous version to a newer version. 

Instructions include config file changes, database schema changes, etc.

## v1.1.3 to 1.1.4
### backend
Add these to `application.properties`:

```
# cflowd for VPLS endpoints: enabled, disabled, not-supported
# should be "not-supported" for ESnet NSO ver <= 1.40.0 
nso.cflowd=not-supported

# set this to true if NSO devices do not support live-status (typically in local ocd-stack development)
nso.mock-live-show-commands=false

# string value that should match the NSO routing-domain identifier
nso.routing-domain=esnet-293
```
