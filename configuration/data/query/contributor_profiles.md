# Query: Contributor Profiles

**Contributor Profiles** is a query that return data from `contributor_profile` type. 

## URL parameters

##### `query`

Can be used for full-text search across contributor profiles. Is searching on top
of `sys_contributors` and `sys_title` fields.
If no `query` parameter is provided then all contributor profiles will match.

**Example:**

- <http://dcp_server:port/v2/rest/search/contributor_profiles?query=pete>

##### `contributor`

Is used to narrow the scope the query will be executed against.
It **MUST match `sys_contributors`** value.
Can be used multiple times.

**Example:**

Get Pete's profile:

- <http://dcp_server:port/v2/rest/search/contributor_profiles?contributor=Pete+Muir+<pmuir%40bleepbleep.org.uk>>

## Query output format

Matching documents are returned in `hits.hits[]`. Every document contains `fields` section.

## Query implementation details

This chapter discusses implementation details of Elasticsearch query. It should be considered _Expert Only_ chapter.

Unescaped mustache template:

          {
            "size": 30,
            "fields": [ "sys_contributors", "sys_title", "sys_url_view" ],
            "script_fields": {
              "accounts": {
                "script": "(_source.accounts && _source.accounts.length > 0 ) ? _source.accounts[0] : ''"
              }
            },
            "query": {
              "filtered": {
                {{#contributor}}
                "filter": {
                  "terms": {
                    "sys_contributors": [
                      {{#contributor}}
                      "{{.}}",
                      {{/contributor}}
                      {}
                    ]
                  }
                },
                {{/contributor}}
                "query": {
                  {{#query}}
                  "simple_query_string": {
                    "fields": ["sys_contributors.fulltext", "sys_title"],
                    "query": "{{query}}"
                  }
                  {{/query}}
                  {{^query}}
                  "match_all": {}
                  {{/query}}
                }
              }
            }
          }
          
To get `accounts` data we are using `script_fields` (see [#232](https://github.com/searchisko/searchisko/issues/232)).          
          
There are some dirty hacks used to workaround Mustache limitations.

- Since `contributors` param is an array there is no easy way how to conditionally include the
  filter for it and populate the array at the same time. Current mustache template repeat complete
  whole filter as many times as there are items in the `contributor` array. Fortunately it seems
  it is not harming ES (but this can be sensitive to changes in ES query parser in the future).
  
- Items in the `contributor` array are delimited by comma. To eliminate parser error we include
  empty object after the last item in the array. This trick seems to work fine for ES parser now.