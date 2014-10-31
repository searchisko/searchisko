# Release Notes

This document contains high-level release notes. More details can be found in [milestones](https://github.com/searchisko/searchisko/issues/milestones) on GitHub.

## 2.0.0-SNAPSHOT

Breaking:

- API URL starts with /v2 [#171](https://github.com/searchisko/searchisko/issues/171) 
- Search API switched from `query_string_query` to `simple_query_string` type.
 This change is needed to support improved security model. [#140](https://github.com/searchisko/searchisko/issues/140) 
- Search API switched from Facets to Aggregations.
 This change is related to deprecation of Facets in Elasticsearch 1.3. [#144](https://github.com/searchisko/searchisko/issues/144)

Improvements:

- security - additional roles for authenticated user [#137](https://github.com/searchisko/searchisko/issues/137), [#147](https://github.com/searchisko/searchisko/issues/147)
- security - brand new Content security subsystem: 
  - support for 'Content type level' security [#142](https://github.com/searchisko/searchisko/issues/142)
  - support for 'Document level' security [#143](https://github.com/searchisko/searchisko/issues/143)
  - support for 'Field level' security [#150](https://github.com/searchisko/searchisko/issues/150)
- security - CORS support is now configurable so you can restrict Origins who can use REST API at all and as authenticated user [#160](https://github.com/searchisko/searchisko/issues/160)
- Administrator can lock "Content manipulation API" during maintenance [#109](https://github.com/searchisko/searchisko/issues/109)
- Added pagination support (from,size) on Feed API [#152](https://github.com/searchisko/searchisko/issues/152)
- New task for syncing all uses from contributor profile provider [#141](https://github.com/searchisko/searchisko/issues/141)
 
Internal:

- Upgrade to elasticsearch 1.3.0 [#48](https://github.com/searchisko/searchisko/issues/48)

## 1.1.1

Improvements:

- Replicate social profile links from Jive to contributor\_profile [#132](https://github.com/searchisko/searchisko/issues/132)

Bug fixes:

- Fix `JdbcContentPersistenceService` unique id constraint violation during content store operation [#129](https://github.com/searchisko/searchisko/issues/129)
- Change mapping of `/indexer/_all/_status` REST end point from `POST` to `GET` [#119](https://github.com/searchisko/searchisko/issues/119)
- Fix bad format of `/indexer/_all/_status` REST end point JSON response [#133](https://github.com/searchisko/searchisko/issues/133)
- Prevent two "Access-Control-Allow-Origin" headers on /v1/rest/sys/es/search/ REST API [#122](https://github.com/searchisko/searchisko/issues/122)

Internal:

- Improved handling of JSON data in unit tests [#134](https://github.com/searchisko/searchisko/issues/134),[#135](https://github.com/searchisko/searchisko/issues/135),[#136](https://github.com/searchisko/searchisko/issues/136)

## 1.1.0

- Security improved to use standard JAAS role based access control [#76](https://github.com/searchisko/searchisko/issues/76)
- ['Normalization API'](http://docs.jbossorg.apiary.io/#normalizationapi) added [#90](https://github.com/searchisko/searchisko/issues/90)
- ['Content Indexers API'](http://docs.jbossorg.apiary.io/#contentindexersapi) extended by `_stop` and `_restart` operations. `_status`, `_stop` and `_restart` can be used for one indexer or all indexer at all also. [#91](https://github.com/searchisko/searchisko/issues/91)
- `Jive6ContributorProfileProvider` improved to process LinkedIn username [#100](https://github.com/searchisko/searchisko/issues/100)
- Updated `elasticsearch-river-remote`, `elasticsearch-river-jira` and `structured-content-tools` to latest versions
- Created ProjectMappingPreprocessor and ContributorMappingPerprocessor to ease content provider and normalizations configuration [#103](https://github.com/searchisko/searchisko/issues/103) 
- Added Audit Log [#104](https://github.com/searchisko/searchisko/issues/104)
- Added access to full Elasticsearch REST API for authenticated administrators [#98](https://github.com/searchisko/searchisko/issues/98)  
- Project Name suggestion API accepts list of fields to return [#93](https://github.com/searchisko/searchisko/issues/93)
- Project Info documents can be search on value of `archived` field [#94](https://github.com/searchisko/searchisko/issues/94)

## 1.0.2

- Filtering by document fields has been generalized [#41](https://github.com/searchisko/searchisko/issues/41)
- 'Content Manipulation API' POST operation returns warnings from data preprocessors
- 'Content Manipulation API' extended by bulk POST and DELETE operations
- Contributor record now contains full name replicated from profile. Useful for contributor normalization by name. 
  **IT IS NECESSARY** to load new mapping for `sys_contributors` index during upgrade, and then run `update_contributor_profile` Task for `jbossorg_username` to fill names from profile.
- Added two new admin tasks `reindex_contributor` and `reindex_project` useful to rebuild search indices for Project and Contributor configurations
- Added ['Content Indexers API'](http://docs.jbossorg.apiary.io/#contentindexersapi) which allows force reindex and get status for content indexers hosted in Searchisko
- Updated elasticsearch-river-remote and elasticsearch-river-jira to latest versions
- Integration tests on REST API [#12](https://github.com/searchisko/searchisko/issues/12)

## 1.0.1

- Bug fix in `update_contributor_profile` task. [#65](https://github.com/searchisko/searchisko/issues/65)
- `jboss-developer` content provider configuration moved to different repo.
- Updates in DCP configuration.

## 1.0.0

- Initial release
