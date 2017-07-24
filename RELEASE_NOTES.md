# Release Notes

This document contains high-level release notes. More details can be found in [milestones](https://github.com/searchisko/searchisko/milestones) on GitHub.

## 2.1.8

## 2.1.7
- [ORG-3643] #290 - Fixed login modules injections so that the app can be compatible with the newest 6.4.14 EAP release.
- [ORG-3677] #291 - structured-content-tools update to 1.3.10 (also jira and remote rivers updated due this) to bring REST call preprocessor 
- [ORG-3694] #296 - Documented logging settings to mute 'Cannot get property...' warnings
- [ORG-3679] #293 - Turned off stats collecting for all environments.

## 2.1.6
- Deadlock while adding RHT user profile in concurrent push [#271](https://github.com/searchisko/searchisko/issues/271)
- java 8

## 2.1.5
- updated elasticsearch-river-remote dependency to v1.6.8

## 2.1.4
- updated elasticsearch-river-remote dependency to v1.6.7
- documentation of "documentation" sys_type

## 2.1.3

- updated elasticsearch-river-remote dependency to v1.6.6
- added registered queries guide

## 2.1.2

Bug fixes:

- Ratings issue while merging contributors [#272](https://github.com/searchisko/searchisko/issues/272)

## 2.1.1

- Minor bugfix release, see [v2.1.1 milestone](https://github.com/searchisko/searchisko/issues?q=milestone%3A2.1.1+is%3Aclosed)
- For full details compare [v2.1.0...v2.1.1](https://github.com/searchisko/searchisko/compare/v2.1.0...v2.1.1)

## 2.1.0

Improvements:

- JMX REST API [#257](https://github.com/searchisko/searchisko/issues/257)
- Support HTTP Basic Authentication within CORS [#259](https://github.com/searchisko/searchisko/issues/259)

Several other bug-fixes and enhancements, see [full list](https://github.com/searchisko/searchisko/issues?q=milestone%3A2.1.0+is%3Aclosed).

## 2.0.2

Improvements:

- Custom tags [#87](https://github.com/searchisko/searchisko/issues/87)
- Expose persistence status info via REST API [#241](https://github.com/searchisko/searchisko/issues/241)

Internal:

- Move configuration to separate repo [#71](https://github.com/searchisko/searchisko/issues/71)

Several other bug-fixes and enhancements, see [full list](https://github.com/searchisko/searchisko/issues?q=milestone%3A2.0.2+is%3Aclosed).

## 2.0.1

Improvements:

- Swap order of filters in "sys\_tags" analyzer [#224](https://github.com/searchisko/searchisko/issues/224)
- Make project name queries to accept list of projects to search in [#223](https://github.com/searchisko/searchisko/issues/223)

Bug fixes:

- Enable clustering [#225](https://github.com/searchisko/searchisko/issues/225)

## 2.0.0

Breaking:

- API URL starts with /v2 [#171](https://github.com/searchisko/searchisko/issues/171)
- Suggestions search APIs were removed [#196](https://github.com/searchisko/searchisko/issues/196). It was replaced by
 specific set of registered queries.
- Search API switched from `query_string_query` to `simple_query_string` type.
 This change is needed to support improved security model. [#140](https://github.com/searchisko/searchisko/issues/140), [#185](https://github.com/searchisko/searchisko/issues/185)
- Search API switched from Facets to Aggregations.
 This change is related to deprecation of Facets in Elasticsearch 1.3. [#144](https://github.com/searchisko/searchisko/issues/144)
- It is also possible that some of the [Elasticsearch 1.0 breaking changes](http://www.elastic.co/guide/en/elasticsearch/reference/1.4/breaking-changes-1.0.html)
 will apply to you (for example returned `fields` values are now [always returned as array](http://www.elastic.co/guide/en/elasticsearch/reference/1.4/_return_values.html)) 

Improvements:

- security - additional roles for authenticated user [#137](https://github.com/searchisko/searchisko/issues/137), [#147](https://github.com/searchisko/searchisko/issues/147)
- security - brand new Content security subsystem: 
  - support for 'Content type level' security [#142](https://github.com/searchisko/searchisko/issues/142)
  - support for 'Document level' security [#143](https://github.com/searchisko/searchisko/issues/143), [#204](https://github.com/searchisko/searchisko/issues/204)
  - support for 'Field level' security [#150](https://github.com/searchisko/searchisko/issues/150)
- security - CORS support is now configurable so you can restrict Origins who can use REST API at all and as authenticated user [#160](https://github.com/searchisko/searchisko/issues/160)
- Administrator can lock "Content manipulation API" during maintenance [#109](https://github.com/searchisko/searchisko/issues/109)
- Added pagination support (from,size) on Feed API [#152](https://github.com/searchisko/searchisko/issues/152)
- New task for syncing all uses from contributor profile provider [#141](https://github.com/searchisko/searchisko/issues/141)
- New Registered query API [#123](https://github.com/searchisko/searchisko/issues/123)
- Enabled scripting support for Javascript [#214](https://github.com/searchisko/searchisko/issues/214)

Internal:

- Upgrade to elasticsearch 1.4.1 [#194](https://github.com/searchisko/searchisko/issues/194) (related is also [#48](https://github.com/searchisko/searchisko/issues/48))
- Using custom build of Elasticsearch 1.4.1 [#195](https://github.com/searchisko/searchisko/issues/195)

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
