Contributor
===========

Contributor configuration contains distinct informations/identifiers used 
by preprocessors in 'Content Push API' to normalize project identifier 
for `dcp_contributors` field.

It's managed over 'Management API - contributors'.

To allow searches for normalization processing by preprocessors, contributor 
configuration documents are stored in the DCP back-end elasticsearch search 
index named `dcp_contributors` as type named `contributor`.

Contributor configuration fields:

* `code` - DCP wide unique contributor identifier. It is stored into 
  `dcp_contributors` field in content pushed into DCP and used as 
  project identifier for Search API filters.
* `email` - array of all email addresses used by this person for community 
  content creation (used for code search during normalization on Content Push API). 

Example of contributor configuration:

	{
	  "code" : "John Doe <john@doe.org>",
	  "email" : ["john@doe.org", "john.doe@gmail.com"]
	}
