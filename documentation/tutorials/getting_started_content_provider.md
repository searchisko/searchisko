# Hello Content Provider!

## Abstract

This is a "Getting Started" for content providers. Searchisko is installed and running (i.e. EAP is running and Searchisko WAR is deployed) and you want to index documents into it and start searching. This documentation explains the steps that need to be done in order to utilise full power of Searchisko from the content provider POW.

## Content

1. Create a new content provider
2. How to prepare your documents
3. Push documents into Searchisko
4. Search documents

### Who owns the data?

Documents in Searchisko can be basically searched and retrieved by anyone how has access to the [REST API](http://docs.jbossorg.apiary.io/) because both the [search](http://docs.jbossorg.apiary.io/#searchapi) and [get](http://docs.jbossorg.apiary.io/#contentpushapi) operation does not require any authorization. However, every document that needs to be indexed into Searchisko has to be *owned* by a *content provider*.

### 1. Create a new content provider

Content provider is someone who can [POST](http://docs.jbossorg.apiary.io/#contentpushapi) new content into Searchisko (or update existing content or delete it). A new content provider gets created via [content provider management API](http://docs.jbossorg.apiary.io/#managementapicontentproviders).

So first of all you need to create a valid [Content provider specification](https://github.com/searchisko/searchisko/blob/master/documentation/rest-api/management/content_provider.md) JSON file and ask system admin to push it for you. Or you can push it yourself if you are also an admin.

* Note that important part of the content provider specification is section with processing rules (see `input_preprocessors`) that are used for various pre-processing before indexing.
* It is up to the admin to double-check that underlying Elasticsearch indices for all `sys_content_type` are unique and do not clash with already existing indices.

### 2. How to prepare your documents

Every document that gets pushed into the system via REST API can undergo series of operations and transformations. As a result some of the field values can be normalized (like contributor name, project name) or a new fields can be calculated. This is important for correct search, aggregations and filtering across all content stored in the system.

For instance: "Calculation of histogram of last update date of documents (relevant to actual search query) for particular contributor across specified projects". In such example it is important to make sure that the _last update date_ value I can be found correctly and both the _contributor_ and _project_ values are normalised.

The fields that are used for common calculations during search (like mentioned aggregations or filtering) are typically prefixed with `sys_` prefix (e.g. `sys_contributors`, `sys_last_activity_date` … etc).

* For complete list of common `sys_` fields see [Content object](https://github.com/searchisko/searchisko/blob/master/documentation/rest-api/content/dcp_content_object.md) and for further usage examples see [available sys_types](https://github.com/searchisko/searchisko/blob/master/documentation/rest-api/README.md).

You, as a content provider, can directly provide  documents that contain fields with these values (if you really know what you are doing) or you can have Searchisko populate/create these fields for you according to pre-defined input processors (that is recommended way).

* See [Content Structure Tools](https://github.com/jbossorg/structured-content-tools) for complete documentation about available input processing tools and see some further example of particular usages - [TBD](#tbd).
* Note that documents can be automatically reindex under the hood in Searchisko (for example when project name is changed or some of contributor's ids is updated/added). The benefit of input processing rules is that reindexed documents will reflect the changes correctly.

### 3. Push documents into Searchisko

For now there are to means of getting documents into the system:

* **REST API** - the recommended approach. Allows to index one document at a time. You can use threads to push more documents in parallel. Every document undergo pre-defined processing operations before actual indexing.
* **Elasticsearch river** - an experimental approach. We can use [Elasticsearch river](http://www.elasticsearch.org/guide/en/elasticsearch/rivers/1.3/index.html) to pull data from external resources. Currently, there are two implementations that we are using ([Remote river](https://github.com/searchisko/elasticsearch-river-remote) and [JIRA river](https://github.com/searchisko/elasticsearch-river-jira)). These rivers need to be installed by system admin. They allow for faster indexing. The disadvantage is higher maintenance and configuration cost. Please contact jboss.org team if you want to know more about this option.

### 4. Search documents

You search for documents via [search API](http://docs.jbossorg.apiary.io/#searchapi). You can provide a lot of URL parameters to fine-tune the output, like getting highlighted snippets, aggregations, pagination .. etc. For example if you are interested only in "your" content you can provide `content_provider` URL parameter as part of the search request.

Simply put, you can push your documents into centralized system and get hosted-search service for data related to your project only while the central search service (e.g. [search.jboss.org](http://search.jboss.org/)) will benefit from your data as well.

For now the search API is quite simple proxy to more powerful [Elasticsearch search API](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/search.html). If it is missing some important features, feel fee to open a [new issue](https://github.com/searchisko/searchisko/issues).

Note that due to _near-real time_ aspect of Lucene it can take some time for indexed documents to become available via search API. In Elasticsearch that time is 1 sec by default (see [index.refresh_interval](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/index-modules.html)).

## Conclusion:

In this tutorial we have outlined the basic steps that needs to happen to define an authority that can push new documents into the system. We have also provided basic references about why and how the documents can be processed and, finally, how to search the documents. 




