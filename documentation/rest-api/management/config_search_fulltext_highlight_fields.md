Searchisko configuration - definition of fields used for highlight during fulltext search
==================================================================================

**[configuration API](http://docs.jbossorg.apiary.io/#managementapiconfiguration) id:** `search_fulltext_highlight_fields`

This configuration document contains Map structure where keys are names of index fields used for highlight during fulltext search in Searchisko 'Search API' `search` operation. 
Value is Map containing three mandatory parameters for field highlight configuration called `fragment_size`, `number_of_fragments`,`fragment_offset`, 
see [ElasticSearch Highlight documentation](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/search-request-highlighting.html).

Example:

	{
	  "sys_title": { "fragment_size" : "-1", "number_of_fragments" : "0", "fragment_offset" : "0"},
	  "sys_description":  { "fragment_size" : "-1", "number_of_fragments" : "3", "fragment_offset" : "20"},
	  "sys_contributors.fulltext":  { "fragment_size" : "-1", "number_of_fragments" : "0", "fragment_offset" : "0"}
	}