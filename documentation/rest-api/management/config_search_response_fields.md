DCP configuration - definition of search response fields
========================================================

**configuration API id:** `search_response_fields`

This configuration document contains array with default set of fields returned from DCP 'Search API' `search` operation if set of fields is not defined in request. See related [ElasticSearch documentation](http://www.elasticsearch.org/guide/reference/api/search/fields.html) for details.

Example:

	{
	  "search_response_fields" : ["_id","sys_type","sys_title","sys_url_view", "sys_description", "sys_project", "sys_project_name", "sys_tags", "sys_contributors", "sys_created", "sys_last_activity_date" ]
	}