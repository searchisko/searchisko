Project
=======

Project configuration is used by preprocessors in 'Content Push API' to 
normalize project identifier for `dcp_project` field.
It's managed over 'Management API - projects'.

To allow searches for normalization processing by preprocessors, 
project configuration documents are stored in the DCP back-end 
ElasticSearch search index named `dcp_projects` as type named `project`.

Project configuration fields:

* `code` - DCP wide unique project identifier. It is stored into 
  `dcp_project` field in content pushed into DCP and used as project 
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