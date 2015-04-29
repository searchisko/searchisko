# Query: Suggest Project Name

**Suggest Project Name** is a family of queries to suggest project name(s):

1. `suggest_project_name_ngram`
2. `suggest_project_name_ngram_more_fields`
3. `suggest_project_name_flt`

They differ in internal implementation and can provide different results. Which one to use depends
on particular use case - see below. These queries search on top of `"sys_type": "project_info"`
content. Archived projects are always filtered out.

## suggest_project_name_ngram

Query trying to do **exact match** against project name using [ngrams][] and [edge ngram][] tokens.
 
[ngrams]: http://www.elastic.co/guide/en/elasticsearch/reference/1.4/analysis-ngram-tokenizer.html
[edge ngram]: http://www.elastic.co/guide/en/elasticsearch/reference/1.4/analysis-edgengram-tokenizer.html

## suggest_project_name_ngram_more_fields

The same query as the **suggest_project_name_ngram** except it returns more fields.

## suggest_project_name_flt

Based on [Fuzzy Like This](http://www.elastic.co/guide/en/elasticsearch/reference/1.4/query-dsl-flt-query.html) query.
This query can cope with typos in input. It can be a good query to use when previous queries do not return any results
(to get "Did you mean?" functionality). 

## URL parameters

All three queries accepts the same URL parameters:

##### `query`

Project name or part of it. Casing is not important.
If no `query` parameter is provided then all projects will match.

**Example:**

Doing exact match:

- <http://dcp_server:port/v2/rest/search/suggest_project_name_ngram?query=WildF>

Doing fuzzy match:

- <http://dcp_server:port/v2/rest/search/suggest_project_name_flt?query=WuldF>

##### `project`

Is used to narrow the scope the query will be executed against.
It **MUST match `sys_project`** value (which is lowercase).
Can be used multiple times.

**Example:**

Get all projects from projects infinispan and wildfly that have **exact match** against "fin" query (the result will be infinispan only):

- <http://dcp_server:port/v2/rest/search/suggest_project_name_ngram?query=fin&project=infinispan&project=wildfly>

Get all projects from projects infinispan and wildfly that have **fuzzy match** against "fun" query (the result will be infinispan only):

- <http://dcp_server:port/v2/rest/search/suggest_project_name_flt?query=fun&project=infinispan&project=wildfly>

## Query output format

Matching projects are returned in `hits.hits[]`. Every returned project contains `fields` section (this is the data you
are interested in) and `highlight` section (with highlighted
matching fragments - as of writing highlights are experimental and need improvements).

## Query implementation details

This chapter discusses implementation details of Elasticsearch query. It should be considered _Expert Only_ chapter.

### suggest_project_name_ngram

Unescaped mustache template:

      {
        "fields" : [ "sys_project", "sys_project_name" ],
        "size": 500,
        "query" : {
          "filtered" : {
            "query": {
              {{#query}}
                "multi_match": {
                  "query": "{{query}}",
                  "fields": [ "sys_project_name", "sys_project_name.edgengram", "sys_project_name.ngram" ],
                  "analyzer": "whitespace_lowercase"
                }
              {{/query}}
              {{^query}}
                "match_all": {}
              {{/query}}
            },
            "filter": {
              "and": [
                {
                  "or": {
                    "filters": [
                      {{#project}}
                        { "term": { "sys_project": "{{.}}" }},
                      {{/project}}
                      {}
                      {{^project}}
                        , { "exists": { "field": "sys_project" }}
                      {{/project}}
                    ],
                    "_cache": true
                  }
                },
                {
                  "or": {
                    "filters": [
                      {
                        "and": [
                          { "not": { "term": { "archived": true } } },
                          { "exists": { "field": "archived" } }
                        ]
                      },
                      {
                        "missing": { "field": "archived" }
                      }
                    ],
                    "_cache": true
                  }
                }
              ]
            }
          }
        },
        "highlight" : {
          "fields" : {
            "sys_project_name" : {
              "fragment_size" : 1,
              "number_of_fragments" : 0
            },
            "sys_project_name.ngram" : {
              "fragment_size" : 1,
              "number_of_fragments" : 0
            },
            "sys_project_name.edgengram" : {
              "fragment_size" : 1,
              "number_of_fragments" : 0
            }
          }
        }
      }
      
### suggest_project_name_ngram_more_fields

The same as **suggest_project_name_ngram** except it returns more fields:

      {
        "fields" : [
            "sys_project", "sys_project_name", "archived", "license", "projectName",
            "anonymousGitLink", "anonymousLink", "blogLink", "buildLink", "chatLink", "committerLink",
            "committerGitLink", "communityLink", "description", "devForumLink", "docsLink", 
            "downloadsLink", "fisheyeLink", "githubLink", "hudsonLink", "issueTrackerLink",
            "jiraLink", "knowledgeBaseLink", "mailingListLink", "specialIcon", "srcLink",
            "sys_url_view", "twitterLink", "userForumLink", "viewvcLink"
            ],

        ...
      }
      
### suggest_project_name_flt

Unescaped mustache template:

      {
        "fields" : [ "sys_project", "sys_project_name" ],
        "size": 500,
        "query" : {
          "filtered" : {
            "query": {
              {{#query}}
                "fuzzy_like_this": {
                  "fields": [ "sys_project_name", "sys_project_name.ngram" ],
                  "like_text": "{{query}}",
                  "max_query_terms": 10,
                  "analyzer": "whitespace_lowercase"
                }
              {{/query}}
              {{^query}}
                "match_all": {}
              {{/query}}
            },
            "filter": {
              "and": [
                {
                  "or": {
                    "filters": [
                      {{#project}}
                        { "term": { "sys_project": "{{.}}" }},
                      {{/project}}
                      {}
                      {{^project}}
                        , { "exists": { "field": "sys_project" }}
                      {{/project}}
                    ],
                    "_cache": true
                  }
                },
                {
                  "or": {
                    "filters": [
                      {
                        "and": [
                          { "not": { "term": { "archived": true } } },
                          { "exists": { "field": "archived" } }
                        ]
                      },
                      {
                        "missing": { "field": "archived" }
                      }
                    ],
                    "_cache": true
                  }
                }
              ]
            }
          }
        },
        "highlight" : {
          "fields" : {
            "sys_project_name" : {
              "fragment_size" : 1,
              "number_of_fragments" : 0
            },
            "sys_project_name.ngram" : {
              "fragment_size" : 1,
              "number_of_fragments" : 0
            },
            "sys_project_name.edgengram" : {
              "fragment_size" : 1,
              "number_of_fragments" : 0
            }
          }
        }
      }
      
