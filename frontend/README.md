# oscars-frontend

A frontend for OSCARS 1.0

## Building blocks
- React (v16.4.2)
- MobX (v5.0.0)
- React-Bootstrap (v4.1.3)
- vis.js (v4.21.0)


## Developer guide

### Installing prerequisites

`npm install` (or, `mvn package`)

### Bring up a dev server 

`npm run start`

### Packaging for Maven deploymentState 

`mvn clean package`

## Development notes

- The version number for the module is important; it should be the same in both these files:
  - `pom.xml`
  - `package.json` 
  - `package-lock.json` 
  
You will only need to manually change the version number in in `pom.xml`. 
After an `mvn package` or similar is run, `package[-lock].json` will be updated
automatically. Remember to commit the updated files.

 

### IntelliJ IDEA settings 

- Plugins
  - Install NodeJS plugin, restart IDEA  
- Preferences -> Languages & Frameworks
  - Node.js and NPM: Enable
  - Javascript: Set project language to React JSX
  - Javascript -> Libraries
    - Download
    - Select 'jasmine', Download and install, Apply
