Distributed Contribution Platform API
=====================================

This project contains implementation of API for Distributed Contribution Platform (DCP) and main DCP documentation.

Documentation for DCP is placed in this repo in the [`documentation`](documentation) folder.

DCP API is Java EE 6 application intended to run in the JBoss EAP 6 application server.

## DCP Overview

**Distributed Contribution Platform** is a system intended to store and 
search/retrieve information related to distinct JBoss Community OSS projects, 
aggregated from various source systems. 
This system is necessary to support/extend synergy of JBoss Community in the 
era of more distributed development environment, when project teams tends to 
use third party systems instead of systems provided and maintained by JBoss 
Community Team.

Distributed Contribution Platform main design attributes:

* high availability of services (both for common runtime and platform upgrade time)
* simple, quick and flexible search of stored informations
* possibility to store informations with guaranteed long term persistence for data sources where it is hard or impossible to obtain informations again (eg. blog posts obtained over RSS protocol)
* high flexibility of stored information structures
* openness and easy use by other community members

To support synergy, informations stored into DCP will be normalized in these areas:

* information type - so all pieces of informations of same type (blog post, issue, commit) originated from distinct systems can be obtained by one search request
* project - so all informations/contributions related to one project can be obtained by one search request
* contributor - so all informations/contributions performed by one contributor can be simply obtained by one search request
* tags - so you can obtain all pieces of informations tagged with same value
* activity date - so you can filter/analyze informations/contributions by dates when they was created/updated   

**Distributed Contribution Platform** provides **REST API** for informations manipulation and search/retrieve, see [`documentation`](documentation).
