Searchisko configuration - definition of fields used for fulltext search
==================================================================

**[configuration API](http://docs.jbossorg.apiary.io/#managementapiconfiguration) id:** `search_fulltext_query_fields`

This configuration document contains Map structure where keys are names of index fields used for fulltext search in Searchisko 'Search API' `search` operation. 
Value is float value representing 'boost' for this field. If value is empty no boost is used for given field.

Example:

	{
	  "sys_title": "2.5",
	  "sys_description": "",
	  "sys_project_name": "2",
	  "sys_tags":"1.5",
	  "sys_contributors.fulltext": ""
	}