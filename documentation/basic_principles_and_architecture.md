DCP basic principles and architecture
=====================================

## DCP Overview

**Distributed Contribution Platform** is a system intended to store and 
search/retrieve information related to distinct JBoss Community OSS projects, 
aggregated from various source systems. 
This system is necessary to support/extend synergy of JBoss Community in the 
era of more distributed environment, when project teams tends to 
use third party systems instead of systems provided and maintained by JBoss 
Community Team.

Distributed Contribution Platform main design attributes:

* high availability of services (both for common runtime and platform upgrade time)
* simple, quick and flexible search of stored informations
* possibility to store informations with guaranteed long term persistence for 
  data sources where it is hard or impossible to obtain informations again (eg. blog 
  posts obtained over RSS protocol)
* high flexibility of stored information structures
* openness and easy use by other community members

To support synergy, informations stored into DCP will be normalized in these areas:

* information type - so all pieces of informations of same type (blog post, 
  issue, source code commit, mailing list email) originated from distinct 
  systems can be obtained by one search request
* project - so all informations/contributions related to one project can be 
  obtained by one search request
* contributor - so all informations/contributions performed by one contributor 
  can be simply obtained by one search request
* tags - so you can obtain all pieces of informations tagged with same value
* activity date - so you can filter/analyze informations/contributions by dates
  when they was created/updated   

DCP provides **REST API** for informations manipulation and search/retrieve, 
see [`REST API documentation`](rest-api/README.md).

## Basic principles

### Flexible data structure for content
A document stored into the DCP platform will be stored in the same format 
as received (JSON format) with all fields provided. 
Some 'system data fields' with unique nonconflicting names will be added 
into this document before it is stored in the DCP,
see [details here](rest-api/content/dcp_content_object.md).

We can define other "system data fields" specific for the concrete content types. 
For example content type "issue" can store issues from JIRA, GitHub, Bugzilla, 
but we define new system fields for them with normalized values, such as 
"issue type" (bug, feature request), "issue status" (new, in progress, resolved, closed) etc.

Documents without "system data fields" can be stored into platform, but will not 
be visible/searchable over basic search GUI at search.jboss.org. 
They will be available over search API to be used by users who need them.

 
### Flexible normalization process
Normalization of the values for `dcp_contributors` and `dcp_project` field must be 
flexible to handle distinct identifiers sent from distinct systems to the normalized unique ones.

Normalization process for contributors:

* contributor is uniquely identified by string `Name Surname <primaryemail@email.com>` 
  containing primary email address. Secondary email addresses used by given contributor 
  and other user identifiers from other systems (eg. jboss.org username, github username) 
  are joined to this primary string in contributor mapping definition.
* contributor mapping definitions are stored in the DCP, see [Contributors mapping documentation]
  (rest-api/management/contributor.md)
* external content provider passes contributor identifier in arbitrary document 
  field, DCP Push API takes it here, looks for unique contributor identifier 
  in mapping and stores it into `dcp_contributors` field and then store document in DCP.  
* when mapping definition for some contributor changes, then all affected 
  documents in DCP search indices must be reindexed. Because document stored 
  in DCP contains all original input fields no external data source need to be 
  called. We can obtain documents from our search indices and update them here.  
  DCP contains universal component called "Reindexator" which is able to perform this process.

Same principle is used for project identifier normalization, see 
[Project configuration documentation](rest-api/management/project.md)

### Long term content persistence support
Guaranteed long term persistence of content is necessary for some data sources, 
where it is hard or impossible to obtain informations again in future. Example 
is blog post obtained over RSS protocol. DCP supports this over the "Persistence back-end" 
component. "Reindexator" component is able to rebuild search indices from data stored here.
 
### Zero downtime upgrade process
We need to develop a zero downtime upgrade process. The basic idea of the
process in case of some significant changes in DCP or underlying technologies:

1. if the platform REST API changes and becomes backward incompatible for the 
   new version, then prepare a testing instance with new API and announce the 
   upgrade to the community, so they can prepare for the migration
2. prepare/install the "new" platform instance
3. disable the information input into the "old" platform instance (disable Push API, 
   stop Indexers), data retrieval/search will still be possible
4. copy configurations and search indexes from the 'old' to the  'new' instance 
   - it can take a long time, depends on data amount
5. switch all "client" systems to the search over the "new" platform instance
6. stop the old instance
7. enable the information input into "new" platform instance (enable Push API, start Indexers)

### Runtime environment
DCP is platform with runtime agnostic architecture, so it can run 
distinct components on any platform and/or cloud provider (OpenShift, EC2 etc).

## DCP Architecture

![DCP Architecture overview](dcp-architecture.png)

### Push API

This component allows to push information into the platform from various 
information sources. This API supports basic CRUD and List operations for content:

* POST document into platform (create or update it based on provided 'provider content type' and id)
* DELETE document from platform (based on provided 'provider content type' and id)
* GET document from platform (based on provided 'provider content type' and id)
* LIST id's all documents of defined 'provider content type' currently available in platform

REST technology is used for this API.

Main API parameters used for document POST request:

* content provider identifier
* password
* provider content type - each content provider has set of 'provider content 
  types' configured. This configuration defines how is the given document handled 
  during the insert into the platform.
* provider content identifier - unique identifier of content per given 'provider content type'
* provider content document - JSON formatted document to be inserted into platform

Main actions performed during document POST request handling:

* authentication - only an authorized request can insert data - the 
  authorization is performed over "content provider identifier" and "password"
* content processing necessary to produce defined 'dcp_type' document - based on 
  configuration for given 'provider content type' - adds 'system data fields' with 
  the values transformed over the 'data normalization' process etc.
* storage routing - based on the 'provider content type' configuration - stores 
  the data to a Search Back-end and a Persistence Back-end. It defines which 
  search index will be used in the Search Back-end  etc.

So before some third party starts to use DCP it must negotiate a new 'content provider' 
access with DCP administrators. Both parties must agree on the set of 'provider content 
type's for this access and define the configuration for each of it. 
See [Content provider documentation](rest-api/management/content_provider.md).


Framework for mappings/normalizations has been developed as part 
of ElasticSearch JIRA river and extracted to separate github 
project called [structured-content-tools](https://github.com/jbossorg/structured-content-tools).

### Search API

It is a component that allows to retrieve/search data from the platform. It is 
used by both JBoss Community Team developed frontend systems (basic search GUI 
at search.jboss.org, new Borg at planet.jboss.org ) and third party/project teams 
developed systems also.

Current search API supports predefined set of filters and facets. Full ElasticSearch search 
API should be provided in the future, but we need to investigate if this is not dangerous 
from some point of view, eg. stoling of all email addresses, performance etc.

We also need to implement some form of QoS handing here - for search requests from unauthorized/unknown systems so they do not degrade the performance of the authorized/known systems.

Part of this API will be the "Project list" operation too that allows to obtain the list of project identifiers and related project names used in platform normalized field dcp_project.
 
### Search Back-end

Fulltext search nodes with distributed search indices etc.

Based on ElasticSearch technology - [http://www.elasticsearch.org](http://www.elasticsearch.org).

Informations are not persistently stored here, because search indices must be 
rebuilt from scratch from data sources in some cases (typically when something significant 
is changed in ElasticSearch or Lucene).

Separate ElasticSearch cluster is used to store some statistics from DCP runtime for use by DCP administrators.

### Persistence Back-end

It's component realizing long term persistence for selected content from data 
sources where is hard or impossible to obtain the information again in future 
(eg. blog posts obtained over RSS protocol).

It is used by Reindexator component to rebuild the search indices on Search Back-end from scratch.

Some form of distributed key/value store (where value is JSON document) or SQL 
database should be used here.

Part of Persistence Back-end is used to store DCP configuration data also.
 
### Indexers

These components run code provided and maintained by DCP Administrators that 
acquires the information from an external data source and store it into the platform.

They can be implemented as ElasticSearch rivers, or as standalone processes using "DCP Push API".
For example instances of [JIRA River Plugin for ElasticSearch](https://github.com/jbossorg/elasticsearch-river-jira) 
pulling data from distinct JIRA instances (issues.jbss.org, hibernate.onjira.com etc). 

This is an alternative of information retrieval to pushing by third party systems over "Push API".
 
### Reindexator

This component performs search index updates in two main cases:

 * reindex data in Search back-end affected by changes in normalization mappings
   (for example new email address is added to some contributor mapping definition)
 * reindex data from Persistence back-end into Search back-end indices
 
### Management API

REST API for DCP management operations. Used by DCP Administrators directly or over Administrative GUI.
 
### Administrative GUI

Web GUI application used by DCP Administrators to configure and monitor whole platform. Main use cases:

* platform runtime parameters monitoring, eg. [bigdesk for ElasticSearch](http://bigdesk.org) 
  and visualization of data stored in statistics part of Search back-end 
* platform configuration, eg. 'Push API' configuration (eg. providers), "Indexers" configuration etc.
* change of mappings for values normalization, start search index update for data affected by changed mapping
* start reindexation of data from Persistence back-end into Search back-end
* data migration to new version of platform during upgrade
