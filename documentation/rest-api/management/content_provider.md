Content provider
================

*Content provider* is an entity that stores content into Searchisko (see Content Manipulation API).
Configuration for the *content provider* contains some descriptive/contact information, information 
for authentication/authorization on the Searchisko REST API, and configuration of content types pushed 
into Searchisko by this provider.
It's managed over 'Management API - content providers'.

Content provider configuration fields:

* `name` - Searchisko instance wide unique name of the provider. It is stored into `sys_content_provider` field in content pushed by this provider. Also used for authentication on Searchisko REST API.
* `description` - description of the provider.
* `contact_email` - contact email for responsible person on provider side, so Searchisko admin can contact he/she.
* `super_provider` - if `true` then this provider has 'Searchisko administrator' role on the Searchisko REST API.
* `pwd_hash` - password hash for authentication on the Searchisko REST API. This field is accepted only during provider creation and never returned back over `get` operations. Special REST API operation must be used to change password later.  
* `type` - structure with configuration of content types pushed into Searchisko by this provider. Key in the structure is type name, which **MUST** be Searchisko instance wide unique (typically starts with content provider `name`). It is used on the 'Content Manipulation API' and stored into `sys_content_type` field of content pushed into Searchisko by this provider. Configuration of each type contains these fields:
 * `description` - description of this type, what contains, which system produces it etc.
 * `sys_type` - value stored into `sys_type` field of pushed content (see description in the [Searchisko Content object](../content/dcp_content_object.md) chapter).
 * `sys_visible_for_roles` - array of strings with user roles of users who can obtain documents of this type over search REST API. User with `admin` role can see documents even if not listed there. If field is not present or is empty then documents of this type are visible for all users.
 * `sys_content_content-type` - MIME identifier of content type stored in the `sys_content` field if it is used. Eg. `text/plain`, `text/html`, `text/x-markdown`. Fulltext search analyzer for `sys_content` field must be set correctly in ElasticSearch mapping regarding this type (eg. use of html stripping etc.).
 * `search_all_excluded` - optional, if `true` then documents with this type are excluded from searchings targeted to all documents (so can be searched only by explicit requests for this type)
 * `persist` - optional, if `true` then documents with this type are stored into Searchisko persistent store during push. Search index can be rebuilt from this persistent store. Used for content which is hard or expensive to obtain again in the future.
 * `input_preprocessors` - array of preprocessors applied on content of this type while pushed over 'Content Manipulation API'. 
   Typically used to normalize values into other `sys_` fields as `sys_project`, `sys_contributors`, `sys_activity_dates` etc. [structured-content-tools](https://github.com/jbossorg/structured-content-tools) framework is used here. 
   We have few [Searchisko specific preprocessors](https://github.com/searchisko/searchisko/tree/master/api/src/main/java/org/searchisko/tools/content) also to 
   ease configurations for Project and Contributor mappings etc.
 * `index/name` - name of search index in Searchisko internal Elasticsearch cluster content of this type is stored into during push.  
 * `index/type` - type for mapping in Searchisko internal Elasticsearch cluster content of this type is stored into.
 * `index/search_indices` - array with names of search indices in Searchisko internal Elasticsearch cluster used during searching for this content type. Optional, name from `index/name` field is used for search if this field is not defined.
 * `indexer` - use this configuration section if Searchisko internal indexer is used for this content. Defines which indexer belongs to this content type, and is necessary for Searchisko 'Content Indexers REST API' to work correctly.
 * `indexer/type` - type of indexer used for content. [elasticsearch-river-remote](https://github.com/searchisko/elasticsearch-river-remote) and [elasticsearch-river-jira](https://github.com/searchisko/elasticsearch-river-jira) Elasticsearch rivers are supported now. 
 * `indexer/name` - name of indexer. In case of Elasticsearch rivers it is simply name of the river.  
 
### Note:

The idea behind having both `index/name` and `index/search_indices` defined is to allow for use of Elasticsearch
[index aliases](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/indices-aliases.html)
for index scalability. In Elasticsearch it is possible to push new documents into index alias if that alias name refers
to a single index only. So you can use the alias of name `index/name` for the index that new documents should go into and
alias `index/search_indices` for all the indices that need to participate in search operations.

Example of content provider configuration:

	{
	  "name": "jborg",
	  "description" : "Provider used by jboss.org Development Team to manage DCP and push content from core jboss.org systems",
	  "contact_email" : "help@jboss.org",
	  "super_provider": true,
	  "pwd_hash": "aa345345324523452345234523",
	  "type": {
	    "jbossorg_blog": {
	      "description" : "Blog posts pushed into DCP by Borg system - planet.jboss.org",
	      "sys_type": "blogpost",
	      "sys_content_content-type" : "text/html",
	      "search_all_excluded" : "false",
	      "persist" : true,
	      "input_preprocessors": [
	        {
	          "name": "DCP project mapper - feed to sys_project mapping",
	          "class": "org.jboss.elasticsearch.tools.content.ESLookupValuePreprocessor",
	          "settings": {
	            "target_field": "sys_project",
	            "index_name": "sys_projects",
	            "idx_search_field": "type_specific_code.jbossorg_blog",
	            "idx_result_field": "code",
	            "source_field": "feed",
	            "value_default": "unknown",
	            "index_type": "project"
	          }
	        }
	      ],
	      "index": {
	        "name": "jbossorg_blog",
	        "type": "jbossorg_blogpost",
	        "search_indices": ["jbossorg_blog", "jbossorg_blog_2012"]
	      }
	    },
	    "jbossorg_project_info": {
	      "description" : "Information about projects (name, links, icons, licenses used etc) pushed into DCP by Magnolia CMS",
	      "sys_type": "project_info",
	      "sys_visible_for_roles" : ["role1","role2"],
	      "sys_content_content-type" : "text/plain",
	      "input_preprocessors": [
	        {
	          "name": "DCP project mapper - nodePath to sys_project mapping",
	          "class": "org.jboss.elasticsearch.tools.content.ESLookupValuePreprocessor",
	          "settings": {
	            "target_field": "sys_project",
	            "index_name": "sys_projects",
	            "idx_search_field": "type_specific_code.jbossorg_project_info",
	            "idx_result_field": "code",
	            "source_field": "nodePath",
	            "value_default": "unknown",
	            "index_type": "project"
	          }
	        }
	      ],
	      "index": {
	        "name": "project_info",
	        "type": "jbossorg_project_info"
	      }
	    }
	  }
	}
