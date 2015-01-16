# Registered Query

Registered query can be a good solution where the standard search API is not sufficient
or not a good fit for particular use case. It allows to register search request template
expressed in [Elasticsearch query DSL][] enhanced by templates based on [Mustache][].

Query configuration fields:

* `id` - Unique ID of this registered query.
* `description` - Query description. Optional field.
* `roles` - An array of client [roles](../#roles) that can execute this query. Optional field.
  If no roles are specified the query is considered public. If specific roles are listed then
  an `admin` role is always considered allowed (even if it is not directly listed in this array).
* `template` - The search query template as defined by [Elasticsearch Search Template][] docs.
  Template can be provided either as an JSON object or as escaped string value (the later option
  is especially useful if the template can not be expressed as a valid JSON object - for example
  if it contains Mustache conditional clause).

Example of query configuration:

````json
	{
	  "id" : "get_X_docs",
	  "description" : "Get X number of documents.",
	  "roles" : [ "provider1", "provider2" ],
	  "template" : {
	    "query" : {
	      "match_all" : {}
	    },
	    "size": "{{X}}"
	  }
	}
````

[Elasticsearch query DSL]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-request-query.html#search-request-query
[Elasticsearch Search Template]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/search-template.html
[Mustache]: http://mustache.github.io/mustache.5.html
