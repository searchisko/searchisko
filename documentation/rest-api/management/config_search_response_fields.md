Searchisko configuration - definition of search response fields
===============================================================

**configuration API id:** `search_response_fields`

This configuration document contains distinct configurations related to content fields returned from 
Searchisko content retrieval API's (eg. 'Search API'. 'Feed API'). 
See related [ElasticSearch documentation](http://www.elasticsearch.org/guide/en/elasticsearch/reference/0.90/search-request-fields.html) 
for details which fields are available there.

* `search_response_fields` - an array with default set of fields returned from Searchisko 'Search API' 
`search` operation if set of fields is not defined in the request.
* `field_visible_for_roles` - defines map where key is name of field and value is array of user roles 
this field is returned for. If field is not mentioned there then it is returned to all users. 
Be careful that every field is available in `_source` field also, so you have to restrict visibility 
of this field accordingly also. Also be careful when restricting some common (`sys_` prefixed) fields 
as you may remove some content from Feed API response. All fields are always visible to user with `admin` role.  


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
	    "_source"          : ["admin"],
	    "restricted_field" : ["employee","manager"] 
	  }
	}