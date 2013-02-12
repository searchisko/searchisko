Content provider
================

*Content provider* is an entity that stores content into DCP (see Content Push API).
Configuration for the *content provider* contains some descriptive/contact informations, informations for authentication/authorization on the DCP REST API, and configuration of content types pushed into DCP by this provider.
It's managed over 'Management API - content providers'.

Content provider configuration fields:

* `name` - DCP wide unique name of the provider. It is stored into `dcp_content_provider` field in content pushed by this provider. Also used for authentication on DCP REST API.
* `description` - description of the provider.
* `contact_email` - contact email for responsible person on provider side, so DCP admin can contact he/she.
* `super_provider` - if `true` then this provider has 'DCP administrator' role on the DCP REST API.
* `pwd_hash` - password hash for authentication on the DCP REST API. This field is accepted only during provider creation and never returned back over `get` operations. Special REST API operation must be used to change password later.  
* `type` - structure with configuration of content types pushed into DCP by this provider. Key in the structure is type name, which must be DCP wide unique (typically starts with content provider `name`). It is used on the 'Content Push API' and stored into `dcp_content_type` field of content pushed into DCP by this provider. Configuration of each type contains these fields:
 * `description` - description of this type, what contains, which system produces it etc.
 * `dcp_type` - value stored into `dcp_type` field of pushed content (see description in the [DCP Content object](../content/dcp_content_object.md) chapter).
 * `search_all_excluded` - optional, if `true` then documents with this type are excluded from searchings targeted to all documents (so can be searched only by explicit requests for this type)
 * `persist` - optional, if `true` then documents with this type are stored into DCP persistent store during push. Search index can be rebuilt from this persistent store. Used for content which is hard or expensive to obtain again in the future.
 * `input_preprocessors` - array of preprocessors applied on content of this type while pushed over 'Content Push API'. Typically used to normalize values into other `dcp_` fields as `dcp_project`, `dcp_contributors`, `dcp_activity_dates` etc. [structured-content-tools](https://github.com/jbossorg/structured-content-tools) framework is used here.
 * `index/name` - name of search index in DCP internal ElasticSearch cluster content of this type is stored into during push.  
 * `index/type` - type for mapping in DCP internal ElasticSearch cluster content of this type is stored into.
 * `index/search_indices` - array with names of search indices in DCP internal ElasticSearch cluster used during searching for this content type. Optional, name from `index/name` field is used for search if this field is not defined.

Example of content provider configuration:

	{
	  "name": "jborg",
	  "description" : "Provider used by JBoss Community Team to manage DCP and push content from core jboss.org systems",
	  "contact_email" : "help@jboss.org",
	  "super_provider": true,
	  "pwd_hash": "aa345345324523452345234523",
	  "type": {
	    "jbossorg_blog": {
	      "description" : "Blog posts pushed into DCP by Borg system - planet.jboss.org",
	      "dcp_type": "blogpost",
	      "search_all_excluded" : "false",
	      "persist" : true,
	      "input_preprocessors": [
	        {
	          "name": "DCP project mapper - feed to dcp_project mapping",
	          "class": "org.jboss.elasticsearch.tools.content.ESLookupValuePreprocessor",
	          "settings": {
	            "target_field": "dcp_project",
	            "index_name": "dcp_projects",
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
	      "description" : "Informations about projects (name, links, icons, licenses used etc) pushed into DCP by Magnolia CMS",
	      "dcp_type": "project_info",
	      "input_preprocessors": [
	        {
	          "name": "DCP project mapper - nodePath to dcp_project mapping",
	          "class": "org.jboss.elasticsearch.tools.content.ESLookupValuePreprocessor",
	          "settings": {
	            "target_field": "dcp_project",
	            "index_name": "dcp_projects",
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
