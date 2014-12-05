Contributor
===========

Contributor configuration contains system level informations (like roles) and distinct information/identifiers used
by preprocessors in 'Content Manipulation API' to normalize contributor identifier for `sys_contributors` field.

It's managed over 'Management API - contributors'.

To allow searches for normalization processing by preprocessors, contributor 
configuration documents are stored in the Searchisko back-end ElasticSearch search 
index named `sys_contributors` as type named `contributor`.

Contributor configuration fields:

* `code` - Searchisko instance wide unique contributor identifier. It is stored into 
  `sys_contributors` field in content pushed into Searchisko and used as
  project identifier for Search API filters.
* `email` - array of all email addresses used by this person for community 
  content creation (used for code search during normalization on Content Push API). 
* `name` - full name of the contributor (used for code search during normalization 
  on Content Push API). There is multifield used in search index, where `name` is 
  analyzed as keyword for full match, while `name.fulltext` is tokenized for fulltext search.  
* `hire_date` - date when the contributor has been hired.
* `leave_date` - date when the contributor has left the company.
* `type_specific_code` - Map structure with other identifiers used to map pushed 
  data to this contributor. Key in the structure marks type of identifier (eg. 
  jboss.org username, github username), value in structure is identifier itself 
  used during mapping.
* `roles` - List of additional roles that authenticated user has. 
  Authenticated user has `contributor` role assigned by default.  
  You can use `admin` or other [roles for managemement API](../README.md#roles) there also. 
  Never use `provider` role here.
  Names of other roles are flexible and you can use your own when configuring distinct 
  aspects of Searchisko security like [Content Security](../../tutorials/content_security.md), Custom queries security etc.

Example of contributor configuration:

	{
	  "code"  : "John Doe <john@doe.org>",
	  "email" : ["john@doe.org", "john.doe@gmail.com"],
	  "name"  : "John Doe",
	  "hire_date"  : "2010-10-21",
	  "leave_date" : "2013-11-10",
	  "type_specific_code" : {
	    "jbossorg_username" : "jdoe",
	    "github_username" : "john.doe",
	    "jbossorg_blog": ["jbosstools.John Doe", "aerogear.John Doe"]
	  },
	  "roles" : [ "admin" ]
	}


This information is not considered as public! If you have 
to have some public contributor information (aka contributor profile), then simply create 
necessary Content type and fill it with public contributor data. Searchisko has some support for this, 
see issue #24 and [`contributor_profile`](../content/contributor_profile.md) content type.
