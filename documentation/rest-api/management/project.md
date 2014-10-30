Project
=======

Project configuration is used by preprocessors in 'Content Manipulation API' to
normalize project identifier for `sys_project` field.
It's managed over 'Management API - projects'.

To allow searches for normalization processing by preprocessors, 
project configuration documents are stored in the Searchisko back-end 
ElasticSearch search index named `sys_projects` as type named `project`.

Project configuration fields:

* `code` - Searchisko instance wide unique project identifier. It is stored into 
  `sys_project` field in content pushed into Searchisko and used as project
  identifier for Search API filters.
* `name` - project name 
* `description` - description of the project.
* `type_specific_code` - Map structure with other codes used to map pushed 
  data to this project. Key in structure marks type of code (eg. jboss.org 
  JIRA project key), value in structure is code itself used during mapping.

Example of project configuration:

	{
	  "code": "jbosstools",
	  "name": "JBoss Tools",
	  "description" : "",
	  "type_specific_code" : {
	    "jbossorg_blog": ["jbosstools"],
	    "jbossorg_jira": ["JBIDE"],
	    "jbossorg_mailing_list": [],
	    "jbossorg_project_info": "jbosstools"
	  }
	}
	
This information is not considered as public! If you have 
to have some public project information (aka project profile), then simply create
necessary Content type and fill it with public data - eg. see [`project_info`](../content/project_info.md) content type.	