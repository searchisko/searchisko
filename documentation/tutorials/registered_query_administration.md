# Registered Query administration

[Registered queries](../rest-api/management/query.md) are managed via [Management API - registered queries](http://docs.jbossorg.apiary.io/#reference/management-api-registered-queries).
This API is typically used when Searchisko is deployed or when a new registered queries are added or modified ad-hoc.


## How To Create Registered Query

The most difficult part is creating new queries (and debugging them). It requires good knowledge of Elasticsearch
[Request Body Search](https://www.elastic.co/guide/en/elasticsearch/reference/1.4/search-request-body.html),
[Query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/1.4/query-dsl.html) and
[Aggregations](https://www.elastic.co/guide/en/elasticsearch/reference/1.4/search-aggregations.html).
 
Tool that can help with query construction and validation:
  - [Validate API](https://www.elastic.co/guide/en/elasticsearch/reference/1.4/search-validate.html)
  - [Sense](https://github.com/elastic/sense)
  
Once the registered query is constructed it can be pushed to Searchisko for immediate testing (this will replace previous version
of registered query having the same ID).

If the `template` value contains specific Mustache expressions it can not be expressed as a valid JSON and has to be
  provided as escaped string value instead. There are online tools that can help with this, such as <http://bernhardhaeussner.de/odd/json-escape/>.
  Bit it is hard to read and maintain escaped string so it is highly recommended to create a documentation
  file for each registered query and provide the "original" query there.
  For example see [Query implementation details](https://github.com/searchisko/configuration/blob/master/data/query/connectors.md#query-implementation-details)
  for [connectors](https://github.com/searchisko/configuration/blob/master/data/query/connectors.json#L7) query.
  It is also highly recommended to document all [URL parameters](https://github.com/searchisko/configuration/blob/master/data/query/connectors.md#url-parameters)
  for each query and give examples of use. It can be also useful to list all possible URL parameters in the
  [description](https://github.com/searchisko/configuration/blob/master/data/query/connectors.json#L3) of the query.
  
  
Debugging invalid query can be difficult. However, it will become easier as Elasticsearch improves internal exception
 reports and possibly provide a Query DSL grammar in the future. Though still it can be very challenging if the query
 uses scripting inside (this is really expert topic).