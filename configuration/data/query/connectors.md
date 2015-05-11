# Query: Connectors

**Connectors** is a query that can return list of relevant connectors.
It is possible to sort and filter returned data (see below for details). 

## URL parameters

##### `query`

Value of [prefix query](http://www.elastic.co/guide/en/elasticsearch/reference/1.4/query-dsl-prefix-query.html)
against `sys_title` value. Casing is not important.
If no `query` parameter is provided then all Fuse connectors will match.

**Example:**

- <http://dcp_server:port/v2/rest/search/connectors?query=co>

##### `id`

Is used to narrow the scope the query will be executed against.
It **MUST match `sys_content_id`** value (which is lowercase).
Can be used multiple times.

**Example:**

Get all Fuse connectors from connectors SAP, Salesforce and Twitter that have title **prefix** "sa" (output will include only two: Salesforce and SAP):

- <http://dcp_server:port/v2/rest/search/connectors?query=sa&id=camel-sap&id=camel-salesforce&id=camel-twitter>

##### `target_product`

Optional parameter. If provided then the query is restricted to connectors having at least one of the
fields `target_product_1`, `target_product_2` or `target_product_3` equal to provided value.

**Example:**

- <http://dcp_server:port/v2/rest/search/connectors?target_product=fuse>

##### `sortAlpha`

If this parameter is present and has non-null value then returned documents will be sorted **alphabetically** (asc)
using lowercased `sys_title` value.

If this flag is not present or has null value then documents are sorted by `priority` (asc).

**Example:**

The actual value of this parameter does not matter as long as it is not null.
Thus all the following examples have the same meaning:

- <http://dcp_server:port/v2/rest/search/connectors?sortAlpha=true>
- <http://dcp_server:port/v2/rest/search/connectors?sortAlpha=false>
- <http://dcp_server:port/v2/rest/search/connectors?sortAlpha=John-Doe>

## Query output format

Matching documents are returned in `hits.hits[]`. Right now every document returns the `_source`
field, but going forward we want to specify fields to return in which case the returned documents
will contain `fields` section and no `_source` section.

## Query implementation details

This chapter discusses implementation details of Elasticsearch query. It should be considered _Expert Only_ chapter.

Unescaped mustache template:

        {
          "fields": [ "_source" ],
          "size": 500,
          "query": {
            "filtered": {
              {{#query}}
              "query": {
                "prefix": {
                  "sys_title": "{{query}}"
                }
              },
              {{/query}}
              "filter": {
                "and": [
                  {
                    "or": [
                      {{#id}} { "term": { "sys_content_id": "{{.}}" }}, {{/id}}
                      {}
                    ]
                  }
                  {{#target_product}}
                  ,{
                    "or": [
                      { "term": { "target_product_1": "{{target_product}}" }},
                      { "term": { "target_product_2": "{{target_product}}" }},
                      { "term": { "target_product_3": "{{target_product}}" }}
                    ]
                  }
                  {{/target_product}}
                ]
              }
            }
          },
          "sort": [
            {{#sortAlpha}} {{/sortAlpha}} {{^sortAlpha}} { "priority": { "order": "asc" }}, {{/sortAlpha}}
            { "_script": {
              "script": "_source['sys_title'].toLowerCase()",
              "type": "string",
              "order": "asc"
            }}
          ]
        }
      }