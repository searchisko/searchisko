DCP Content object
==================

This document describes main content object which can be pushed to and retrieved or searched from DCP.
 
DCP Content object is a JSON document with a free structure. There is no 
restriction how many key value pairs must be defined or in what structure.
Some system data fields are defined by DCP, some are added into the content
inside DCP during push. Those data fields are prefixed by `dcp_`:

* `dcp_type` - DCP wide normalized content type - eg. mailing-list email, issue, blogpost, IRC post, commit, discussion thread - system field, always necessary.
* `dcp_id` - content id unique in the whole DCP platform - system field, always necessary. It is constructed during the 'Content Push API' operation from `dcp_content_type` and `dcp_content_id`.
* `dcp_content_provider` - identification of the provider that stored the given data into the platform - system field, always necessary - eg. 'jbossorg', 'seam_project' etc.
* `dcp_content_type` - identifier of the provider defined content type for 'Content Push API'. It is unique in the whole DCP so it starts with `dcp_content_provider`, eg. 'jbossorg_jira_issue', 'jbossorg_blog' etc. 
* `dcp_content_id` -  content identifier is passed in by the provider, it must be unique for the given `dcp_content_type`.
* `dcp_updated` - date of last content update in DCP - system field, always necessary, assigned in 'Content Push API'.
* `dcp_project` - normalized DCP wide identifier of the project - system field - it is used for the project facet and filter in the Search API.
* `dcp_project_name` - human readable name of project based on `dcp_project` identifier - system field. 
* `dcp_contributors` - array of contributing persons, no duplicities in array, persons identifiers normalized during push into DCP - each person represented as string `Name Surname <primaryemail@email.com>` - in Search API used for persons facet and filter.
* `dcp_activity_dates` - array of timestamps (ISO string) representing some activity on the content (when the content was created or changed etc. in source system) - in the Search API used for the time facet and filter.
* `dcp_created` - timestamp (ISO string) representing creation of the content in source system (it's min value from `dcp_activity_dates`), used for sorting on search API.
* `dcp_last_activity_date` - timestamp (ISO string) representing last activity on the content (it's max value from `dcp_activity_dates`), used for sorting on search API.
* `dcp_title` - content title - used to present the document in the basic search GUI results - it can be directly set by the content provider during the push operation.
* `dcp_url_view` - URL where the document can be viewed in its original system in human readable form - used to open the document from the basic search GUI - can be directly set by the content provider during the push.
* `dcp_description` - short text representing the content (up to 400 characters) - used to show the content in the basic search GUI results for queries that do not produce highlights - it can be directly set by the content provider during the push, no html formatting.
* `dcp_content` - complete text representing whole content - Search API fulltext search runs against it - it can be directly set by the content provider during the push, may contain html formatting. Basic search GUI may use it in search result detail view.
* `dcp_content_content-type` - MIME identifier of content type stored in the `dcp_content` field eg. `text/plain`, `text/html`, `text/x-markdown`. Must be negotiated with DCP Admins so fulltext search analyzer for `dcp_content` is set correctly.
* `dcp_tags` - array of tags (Strings) - in the Search API used for facet (tag cloud) and filter - it is not directly pushed by the content provider because we plan a mechanism for additional user defined tags, so we need to rewrite this field internally. The content provider should use `tags` field instead.
* `tags` - tags provided by content provider
* `dcp_comments` - Array of comment for issue. 'Comment data structure' is described below.


'Comment data structure' description:

* `comment_id` - unique identifier of the comment from source system. Optional.
* `comment_body` - iext of the comment
* `comment_author` - info about contributor who created comment. Object which contains at least `dcp_contributor` field with normalized DCP contributor identifier.
* `comment_updater` - info about contributor who updated comment. Object which contains at least `dcp_contributor` field with normalized DCP contributor identifier. 
* `comment_created` - timestamp (ISO string) when the comment was created in the source system.
* `comment_updated` -  timestamp (ISO string) when the comment was updated in the source system.


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
	    "dcp_content" : "Set the <code>port_range</code> on JGroups stacks to <code>1</code> to lock down the ports.",
	    "dcp_content_content-type" : "text/html",
	    "dcp_updated": "2012-12-06T06:34:55.000Z",
	    "dcp_project": "as7",
	    "dcp_project_name": "JBoss AS7",
	    "dcp_contributors": ["John Doe <john@doe.org>", "Pat Mat <pat@mat.org>"],
	    "dcp_activity_dates": ["2012-12-06T06:34:55.000Z", "2012-12-05T01:48:05.000Z"],
	    "dcp_created" : "2012-12-05T01:48:05.000Z",
	    "dcp_last_activity_date" : "2012-12-06T06:34:55.000Z", 
	    "dcp_tags": ["Content_tag1", "tag2", "tag3", "user_defined_additional_tag"],
	    "dcp_comments" : [
        {
          "comment_id"      : "1254652",
          "comment_body"    : "Whole comment text.",
          "comment_author"  : {"dcp_contributor": "John Doe <john@doe.org>"},
          "comment_updater" : {"dcp_contributor": "John Doe <john@doe.org>"},
          "comment_created" : "2012-11-27T13:55Z",
          "comment_updated" : "2012-11-27T14:22Z"
        }
      ]
	}

