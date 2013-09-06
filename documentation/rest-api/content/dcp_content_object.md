DCP Content object
==================

This document describes main content object which can be pushed to and retrieved or searched from DCP.
 
DCP Content object is a JSON document with a free structure. There is no 
restriction how many key value pairs must be defined or in what structure.
Some system data fields are defined by DCP, some are added into the content
inside DCP during push. Those data fields are prefixed by `sys_`:

* `sys_type` - DCP wide normalized content type - eg. mailing list email, issue, blogpost, IRC post, commit, discussion thread - system field, always necessary.
* `sys_id` - content id unique in the whole DCP platform - system field, always necessary. It is constructed during the 'Content Push API' operation from `sys_content_type` and `sys_content_id`.
* `sys_content_provider` - identification of the provider that stored the given data into the platform - system field, always necessary - eg. 'jbossorg', 'seam_project' etc.
* `sys_content_type` - identifier of the provider defined content type for 'Content Push API'. It is unique in the whole DCP so it starts with `sys_content_provider`, eg. 'jbossorg_jira_issue', 'jbossorg_blog' etc.
* `sys_content_id` -  content identifier passed in by the provider, it must be unique for the given `sys_content_type`.
* `sys_updated` - date of last content update in DCP - system field, always necessary, assigned in 'Content Push API'.
* `sys_project` - normalized DCP wide identifier of the project - system field - it is used for the project facet and filter in the Search API.
* `sys_project_name` - human readable name of project based on `sys_project` identifier - system field.
* `sys_contributors` - array of contributing persons, no duplicities in array, persons identifiers normalized during push into DCP - each person represented as string `Name Surname <primaryemail@email.com>` - in Search API used for persons facet and filter.
* `sys_activity_dates` - array of timestamps (ISO string) representing some activity on the content (when the content was created or changed etc. in source system) - in the Search API used for the time facet and filter.
* `sys_created` - timestamp (ISO string) representing creation of the content in source system (it's min value from `sys_activity_dates`), used for sorting on search API.
* `sys_last_activity_date` - timestamp (ISO string) representing last activity on the content (it's max value from `sys_activity_dates`), used for sorting on search API.
* `sys_title` - content title - used to present the document in the basic search GUI results - it can be directly set by the content provider during the push operation.
* `sys_url_view` - URL where the document can be viewed in its original system in human readable form - used to open the document from the basic search GUI - can be directly set by the content provider during the push.
* `sys_description` - short text representing the content (up to 400 characters) - used to show the content in the basic search GUI results for queries that do not produce highlights - it can be directly set by the content provider during the push, no html formatting.
* `sys_content` - complete text representing whole content - it can be directly set by the content provider during the push, may contain html formatting. Basic search GUI may use it in search result detail view.
* `sys_content_content-type` - MIME identifier of content type stored in the `sys_content` field eg. `text/plain`, `text/html`, `text/x-markdown`. Must be negotiated with DCP Admins so fulltext search analyzer for `sys_content` is set correctly.
* `sys_content_plaintext` - if `sys_content` is provided then this field is populated automatically by transformations (thus `dcp_content_content-type` value is important). The goal is to have the content without any markup - Search API fulltext search runs against it and this field is also used for highlighted snippets.
* `sys_tags` - array of tags (Strings) - in the Search API used for facet (tag cloud) and filter - it is not directly pushed by the content provider because we plan a mechanism for additional user defined tags, so we need to rewrite this field internally. The content provider should use `tags` field instead.
* `tags` - tags provided by content provider
* `sys_comments` - Array of comment for issue. 'Comment data structure' is described below.


### 'Comment data structure' description:

<table border="1">
<thead>
  <th>Field</th>
  <th width="70%">Description</th>
</thead>
<tbody>
<tr>
  <td>comment_id</td>
  <td>Optional. Unique identifier of the comment from source system.</td>
</tr>
<tr>
  <td>comment_body</td>
  <td>Text of the comment.</td>
</tr>
<tr>
  <td>comment_author</td>
  <td>Info about Contributor who created comment. 'Contributor data structure' is described below.</td>
</tr>
<tr>
  <td>comment_updater</td>
  <td>Optional. Info about Contributor who updated comment. 'Contributor data structure' is described below.</td>
</tr>
<tr>
  <td>comment_created</td>
  <td>Timestamp (ISO string) when the comment was created in the source system.</td>
</tr>
<tr>
  <td>comment_updated</td>
  <td>Optional. Timestamp (ISO string) when the comment was updated in the source system.</td>
</tr>
</tbody>
</table>

### 'Contributor data structure' description:

<table border="1">
<thead>
  <th>Field</th>
  <th width="70%">Description</th>
</thead>
<tbody>
<tr>
  <td>sys_contributor</td>
  <td>Normalized DCP contributor identifier.</td>
</tr>
<tr>
  <td>email_address</td>
  <td>Optional. Contributor email address from the source system. May be used to lookup normalized value for <code>sys_contributor</code> field during data push.</td>
</tr>
<tr>
  <td>display_name</td>
  <td>Optional. Contributor display name from the source system.</td>
</tr>
</tbody>
</table>


### DCP Content described by example:

Free JSON Structure representing content. It can be one key - value pair or something more structured.
It's defined only by content provider and must contain mandatory fields defined for given `sys_type`.

	{
	    "tags": ["Content_tag1", "tag2", "tag3"],
	
	    "sys_content_provider": "jbossorg",
	    "sys_content_type": "jbossorg_jira_issue",
	    "sys_content_id": "AS7-1254",
	    "sys_id": "jbossorg_jira_issue-AS7-1254",
	    "sys_type": "issue",
	    "sys_title": "AS7-1254 - Set the port_range on JGroups stacks to 1",
	    "sys_url_view": "https://issues.jboss.org/browse/AS7-1254",
	    "sys_description": "Set the port_range on JGroups stacks to 1 to lock down the ports.",
	    "sys_content" : "Set the <code>port_range</code> on JGroups stacks to <code>1</code> to lock down the ports.",
	    "sys_content_content-type" : "text/html",
	    "sys_content_plaintext" : "Set the port_range on JGroups stacks to 1 to lock down the ports.",
	    "sys_updated": "2012-12-06T06:34:55.000Z",
	    "sys_project": "as7",
	    "sys_project_name": "JBoss AS7",
	    "sys_contributors": ["John Doe <john@doe.org>", "Pat Mat <pat@mat.org>"],
	    "sys_activity_dates": ["2012-12-06T06:34:55.000Z", "2012-12-05T01:48:05.000Z"],
	    "sys_created" : "2012-12-05T01:48:05.000Z",
	    "sys_last_activity_date" : "2012-12-06T06:34:55.000Z",
	    "sys_tags": ["Content_tag1", "tag2", "tag3", "user_defined_additional_tag"],
	    "sys_comments" : [
        {
          "comment_id"      : "1254652",
          "comment_body"    : "Whole comment text.",
          "comment_author"  : {"sys_contributor": "John Doe <john@doe.org>", "email_address" : "john@doe.org", "display_name" : "John Doe"},
          "comment_updater" : {"sys_contributor": "John Doe <john@doe.org>", "email_address" : "john@doe.org", "display_name" : "John Doe"},
          "comment_created" : "2012-11-27T13:55Z",
          "comment_updated" : "2012-11-27T14:22Z"
        }
      ]
	}

