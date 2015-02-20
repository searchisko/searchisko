# Query: Suggest Tags Prefix

**Suggest Tags Prefix** query is designed to provide suggestions of `sys_tags` starting with input `query` value.

It executes [terms aggregation] of `sys_tags` field in context of `sys_type:blogpost` content.

[terms aggregation]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4/search-aggregations-bucket-terms-aggregation.html

The following text consists of three parts:

- description of URL parameters accepted by this registered query
- explanation of query output
- expert explanation of the query itself

## URL parameters

##### `query`
Keyword used in [prefix filter]. Example: `query=serv`.

[prefix filter]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4/query-dsl-prefix-filter.html

- <http://dcp_server:port/v2/rest/search/suggest_tags_prefix?query=serv>

## Query output format

Returned data contains suggested `sys_tags` candidates in `aggregations.sys_tags_candidates.buckets[]`.

## Query implementation details

This chapter discusses implementation details of Elasticsearch query. It should be considered _Expert Only_ chapter.

    {
      "size" : 0,
      "query" : {
        "constant_score": {
          "filter": {
            "prefix": { "sys_tags": "{{query}}" }
          }
        }
      },
      "aggregations": {
        "sys_tags_candidates": {
          "terms": {
            "field": "sys_tags",
            "include": {
              "pattern": "^\\Q{{query}}\\E.*",
              "flags": "CASE_INSENSITIVE|UNICODE_CASE"
            }
          }
        }
      }
    }

In aggregation `sys_tags_candidates` we need to filter out all terms that do not start with given `query` value.
The reason for this is that matching documents can have several tags and we need to filter out those that
do not share valid prefix.

It should be noted that the `query` value should be **ALWAYS** lowercased because it is matched against analyzed content.