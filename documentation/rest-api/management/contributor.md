Contributor
===========

Contributor configuration contains distinct information/identifiers used
by preprocessors in 'Content Push API' to normalize contributor identifier 
for `sys_contributors` field.

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
* `type_specific_code` - Map structure with other identifiers used to map pushed 
  data to this contributor. Key in the structure marks type of identifier (eg. 
  jboss.org username, github username), value in structure is identifier itself 
  used during mapping.

Example of contributor configuration:

	{
	  "code" : "John Doe <john@doe.org>",
	  "email" : ["john@doe.org", "john.doe@gmail.com"],
	  "type_specific_code" : {
	    "jbossorg_username" : "jdoe",
	    "github_username" : "john.doe",
	    "jbossorg_blog": ["jbosstools.John Doe", "aerogear.John Doe"]
	  }
	}


This information is not considered as public! If you have 
to have some some public contributor informations (aka contributor profile), then simply create 
necessary Content type and fill it with public contributor data. Searchisko has some support for this, 
see issue #24 and [`contributor_profile`](../content/contributor_profile.md) content type.