# OSCARS

## Synopsis
Short for *On-demand Secure Circuits and Advance Reservation System*, OSCARS is  a freely available open-source product. As developed by the Department of Energy’s high-performance science network ESnet, OSCARS was designed by network engineers who specialize in supporting the U.S. national laboratory system and its data-intensive collaborations. 

This project is a complete redesign of the original OSCARS to improve performance and maintainability. 

## Project Structure
The new OSCARS is a docker-based application, made up of two major components: 
 * The main application (the *backend* module), 
 * And the web UI (the *frontend* module)

The main project directory is structured as follows:

### backend
The main application. Handles reservation requests, determines which path (if any) is available to satisfy the request, reserves network resources, and initiates southbound connections to network configuration agents.

### frontend
A node.js webpack application built on React. 

### deploy
Dockerfiles used for creating the application images. Different files are used for production and development.

### docs
Various documentation; many documents need review and updating as of Nov 2023.


### Telemetry 

OSCARS uses OpenTelemetry, and requires a valid token in application.properties in the `otel.*` configuration section.

See the dashboard at the Stardust dashboard https://eapm1.gc1.dev.stardust.es.net:8200/

Go to "Services" > "oscars-backend"