Searchisko configuration - definition of search response fields
===============================================================

**[configuration API](http://docs.jbossorg.apiary.io/#managementapiconfiguration) id:** `search_response_fields`

This configuration document contains distinct configurations related to content fields returned from 
Searchisko content retrieval API's (eg. 'Search API'. 'Feed API'). 
See related [ElasticSearch documentation](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/search-request-fields.html) 
for details which fields are available there.

* `search_response_fields` - an array with default set of fields returned from Searchisko 'Search API' 
`search` operation if set of fields is not defined in the request. Rules for fields which can be returned 
this way come from [elasticsearch field feature](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-request-fields.html).
* `field_visible_for_roles` - defines map where key is name of field and value is array of user roles 
this field is returned for when requested over 'Search API' `search` operation. If field is not mentioned 
there then it is returned to all users if they ask for it. Rules for fields which can be returned 
this way come from [elasticsearch field feature](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-request-fields.html), 
so it is not necessary to restrict unreachable fields there.
Be careful that every field is available in `_source` field also, so you have to restrict visibility 
of `_source` field at all there (but this limits use of Searchisko a lot), or use `source_filtering_for_roles` to remove sensitive fields from it. 
Also be careful when restricting some Searchisko common (`sys_` prefixed) fields as you may remove some content 
from Feed API response. All fields are always visible to user with `admin` role. 
* `source_filtering_for_roles` - this setting defines restrictions to content of `_source` field returned from Searchisko 'Search API' 
`search` operation. It uses [elasticsearch `_source.exclude` clause](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-request-source-filtering.html).
Setting contains map where key is name of field (dot notation and `*` wildcard can be used there) and value is array of user roles 
this field is returned for. So if user is not in any of given roles then defined pattern is excluded from `_source` field for him.
All fields are always visible to user with `admin` role.


Example:

	{
	  "search_response_fields" : [
	    "_id",
	    "sys_type",
	    "sys_title",
	    "sys_url_view", 
	    "sys_description", 
	    "sys_project", 
	    "sys_project_name", 
	    "sys_tags", 
	    "sys_contributors", 
	    "sys_created", 
	    "sys_last_activity_date" 
	  ],
	  "field_visible_for_roles" : {
	    "restricted_field_1" : ["admin"],
	    "restricted_field_2" : ["employee","manager"] 
	  },
	  "source_filtering_for_roles" : {
	    "*.restricted_field_1" : ["admin"],
	    "*.restricted_field_2" : ["employee","manager"]
	  }
	}
