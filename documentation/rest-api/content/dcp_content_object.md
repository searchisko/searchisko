DCP Content object
==================

This document describes main content object which can be pushed to and retrieved or searched from DCP.
 
DCP Content object is a JSON document with a free structure. There is no restriction how many key value pairs must be defined or in what structure.
Some system data fields are defined by DCP, some are added into the content inside DCP during push. Those data fields are prefixed by `dcp_`:

* `dcp_type` - DCP wide normalized content type - eg. mailing-list email, issue, blogpost, IRC post, commit, discussion thread - system field, always necessary.
* `dcp_id` - content id unique in the whole DCP platform - system field, always necessary. It is constructed during the 'Content Push API' operation from `dcp_content_type` and `dcp_content_id`.
* `dcp_content_provider` - identification of the provider that stored the given data into the platform - system field, always necessary - eg. 'jbossorg', 'seam_project' etc.
* `dcp_content_type` - identifier of the provider defined content type for 'Content Push API'. It is unique in the whole DCP so it starts with `dcp_content_provider`, eg. 'jbossorg_jira_issue', 'jbossorg_blog' etc. 
* `dcp_content_id` -  content identifier is passed in by the provider, it must be unique for the given `dcp_content_type`.
* `dcp_updated` - date of last content update in DCP - system field, always necessary.
* `dcp_project` - normalized DCP wide identifier of the project - system field - it is used for the project facet and filter in the Search API.
* `dcp_project_name` - human readable name of project based on `dcp_project` identifier - system field. 
* `dcp_contributors` - array of contributing persons, no duplicities in array, persons identifiers normalized during push into DCP - each person represented as string `Name Surname <primaryemail@email.com>` - in Search API used for persons facet and filter.
* `dcp_activity_dates` - array of timestamps representing some activity on the content (when the content was created or changed etc. in source system) - in the Search API used for the time facet and filter.
* `dcp_last_activity_date` - timestamp representing last activity on the content (it's max value from `dcp_activity_dates`), used for sorting on search API.
* `dcp_title` - content title - used to present the document in the basic search GUI results - it can be directly set by the content provider during the push operation.
* `dcp_url_view` - URL where the document can be viewed in its original system in human readable form - used to open the document from the basic search GUI - can be directly set by the content provider during the push.
* `dcp_description` - short text representing the content - used to show the content in the basic search GUI results for queries that do not produce highlights - it can be directly set by the content provider during the push, no html formatting.
* `dcp_content` - text representing whole content - Search API fulltext search runs against it - it can be directly set by the content provider during the push, may contain html formatting. Basic search GUI may use it in search result detail view. 
* `dcp_tags` - array of tags (Strings) - in the Search API used for facet (tag cloud) and filter - it is not directly pushed by the content provider because we plan a mechanism for additional user defined tags, so we need to rewrite this field internally. The content provider should use `tags` field instead.
* `tags` - tags provided by content provider


DCP Content described by example:

	{
	    Free JSON Structure representing content. It can be one key - value pair or something more structured.
	    It's defined only by content provider and must contain mandatory fields defined for given 'dcp_type'.
	
	    "tags": ["Content_tag1", "tag2", "tag3"],
	
	    "dcp_content_provider": "jbossorg",
	    "dcp_content_type": "jbossorg_jira_issue",
	    "dcp_content_id": "AS7-1254",
	    "dcp_id": "jbossorg_jira_issue-AS7-1254",
	    "dcp_type": "issue",
	    "dcp_title": "AS7-1254 - Set the port_range on JGroups stacks to 1",
	    "dcp_url_view": "https://issues.jboss.org/browse/AS7-1254",
	    "dcp_description": "Set the port_range on JGroups stacks to 1 to lock down the ports.",
	    "dcp_content" : "Set the port_range on JGroups stacks to 1 to lock down the ports."
	    "dcp_updated": "2012-12-06T06:34:55.000Z",
	    "dcp_project": "as7",
	    "dcp_project_name": "JBoss AS7",
	    "dcp_contributors": ["John Doe <john@doe.org>", "Pat Mat <pat@mat.org>"],
	    "dcp_activity_dates": ["2012-12-06T06:34:55.000Z", "2012-12-05T01:48:05.000Z"],
	    "dcp_last_activity_date" : "2012-12-06T06:34:55.000Z", 
	    "dcp_tags": ["Content_tag1", "tag2", "tag3", "user_defined_additional_tag"]
	}

