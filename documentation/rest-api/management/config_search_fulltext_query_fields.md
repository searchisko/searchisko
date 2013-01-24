DCP configuration - definition of fields used for fulltext search
==================================================================

**configuration API id:** `search_fulltext_query_fields`

This configuration document contains Map structure where keys are names of index fields used for fulltext search in DCP 'Search API' `search` operation. 
Value is float value representing 'boost' for this field. If value is empty no boost is used for given field.

Example:

	{
	  "dcp_title": "2.5",
	  "dcp_description": "",
	  "dcp_project_name": "2",
	  "dcp_tags":"1.5",
	  "dcp_contributors.fulltext": ""
	}