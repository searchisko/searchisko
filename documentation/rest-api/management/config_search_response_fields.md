DCP configuration - definition of search response fields
========================================================

**configuration API id:** `search_response_fields`

This configuration document contains array with default set of fields returned from DCP 'Search API' search operation if set of fields is not defined in request. See related [ElasticSearch documentation](http://www.elasticsearch.org/guide/reference/api/search/fields.html) for details.

Example:

	{
	  "search_response_fields" : ["_id","dcp_type","dcp_title","dcp_url_view", "dcp_description", "dcp_project", "dcp_project_name", "dcp_tags", "dcp_contributors", "dcp_last_activity_date" ]
	}