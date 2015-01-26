# Registered Query

Registered query can be a good solution where the standard search API is not sufficient
or not a good fit for particular use case. It allows to register query template (a.k.a.
[Elasticsearch Search Template]) under unique `id`. Clients can execute registered queries
passing in custom parameters.

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
* `default` - _optional_ default section:
  * `sys_type` - _optional_ value of `sys_type`, string or array of strings
  * `sys_content_type` - _optional_ value of `sys_content_type`, string or array of strings
* `override` - _optional_ override section:
  * `sys_type` - _optional_ name of URL parameter to override the default value of `sys_type`
  * `sys_content_type` - _optional_ name of URL parameter to override the default value of `sys_content_type`

Example of query configuration:

````json
	{
	  "id" : "get_X_docs",
	  "description" : "Get X number of documents.",
	  "roles" : [ "provider1", "provider2" ],
	  "default" : {
	    "sys_type" : [ "type1", "type2" ],
	    "sys_content_type": [ "content_type1" ]
	  },
	  "override" : {
	    "sys_type" : "URL_param_name1",
	    "sys_content_type": "URL_param_name2"
	  },
	  "template" : {
	    "query" : {
	      "match_all" : {}
	    },
	    "size" : "{{X}}"
	  }
	}
````

## Security aspects

Since Searchisko 2.x security is important aspect to keep an eye on. By security we mean
[ROLE based access to REST API] and [content security] allowing to control visibility of data at the
*content type*, *document* or *document field* level.

With registered queries it is more challenging to automatically support all security features.
Person who pushes new registered query into Searchisko (admin) should be aware of possible security risks of
given query template and try to minimize them if needed to the lowest level by proposing better query template
or rejecting the query in extreme cases.

Critical security parts of each registered query configuration are the following:

- `roles` section: Can define required roles the client has to be assigned to in order to be allowed to execute
  this query. See more details above.

- `default` section: This section tells Searchisko which `sys_content_type` or `sys_type` this query will
  be execute against (if not specified otherwise, see below). Note that these default values are still subject
  to ROLE based access rules. If no `default` values are configured then search is executed against
  all `sys_content_type`s except those explicitly excluded (see `search_all_excluded` in [content_provider] config).

- `override` section: Allows to specify names of URL parameters that are used to pass in values of `sys_content_type` or `sys_type`.
  At query execution time, values from those URL parameters are passed into query engine and they *override* values defined
  in `default` section (if there is any). Values obtained from URL parameters are still subject to ROLE based access rules.
  If `override` section is present but client do not use these URL parameters at all, then the `default` section is used.

### Priority of evaluation

The `sys_content_type` has higher priority than `sys_type`. This means that if registered query `default` or `override`
section specify both then at execution time `sys_type` is ignored if `sys_content_type` value apply.

For example, if the `default` section contains configuration of both the `sys_content_type` and `sys_type` then only
the `sys_content_type` apply. If, however, there is `override` section containing configuration for both but only
`sys_type` is provided in URL parameters then `sys_type` is honored even, if there is `sys_content_type` value
configured in the `default` section.

Priorities can be expressed in the following levels:

1. If `override.sys_content_type` is configured and matching value is provided via URL, then this has the highest
   priority and the following levels are skipped.
2. If `override.sys_type` is configured and matching value is provided via URL, then this is used and the following
   levels are skipped.
3. If `default.sys_content_type` is configured, then it is used and the following levels are skipped.
4. If `default.sys_type` is configured, then it is used and the following levels are skipped.
5. Search is executed against all `sys_content_type`s except those explicitly excluded.

### Returning document fields

If not stated otherwise document `_source` is returned by default. This is not always useful and can introduce serious
security risks. Thus it is recommended to use [fields] in search template to name fields to extract. However, registered
queries do not support *field* level security now (this may change in the future). One option how to deal with this limitation
is to register more similar queries that will return different set of fields and set different `roles` for them if needed.

### Document level security

The *document* level security is not supported for now. However, it is possible to make sure the registered query contains
required filters to mimic this functionality.

[Elasticsearch query DSL]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-request-query.html#search-request-query
[Elasticsearch Search Template]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/search-template.html
[Mustache]: http://mustache.github.io/mustache.5.html
[ROLE based access to REST API]: https://github.com/searchisko/searchisko/tree/master/documentation/rest-api#authentication-and-roles-based-rest-api-security
[content security]: https://github.com/searchisko/searchisko/blob/master/documentation/tutorials/content_security.md
[content_provider]: https://github.com/searchisko/searchisko/blob/master/documentation/rest-api/management/content_provider.md
[fields]: http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4/search-request-fields.html#search-request-fields
