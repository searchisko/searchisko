Searchisko Content object
=========================

This document describes main content object, which can be pushed to and retrieved or searched from Searchisko.
 
Searchisko Content object is a JSON document with a free structure. There is no 
restriction about how many key value pairs must be defined or in what structure.
There are however some system data fields defined by Searchisko (prefixed by `sys_`) and they play specific role in the system,
some of them are set during content push before pre-processing, thus they will be overridden if provided by client (see column <b>SET</b>):

<table border="1">
<thead>
  <th>Field</th>
  <th>SET</th>
  <th width="70%">Description</th>
</thead>
<tbody>
<tr>
  <td>sys_type</td>
  <td>yes</td>
  <td>Searchisko wide normalized content type - eg. mailing list email, issue, blogpost, IRC post, commit, discussion thread - system field, always necessary.</td>
</tr>
<tr>
  <td>sys_id</td>
  <td>yes</td>
  <td>Content id unique in the whole Searchisko platform - system field, always necessary. It is constructed during the 'Content Push API' operation from <code>sys_content_type</code> and <code>sys_content_id</code>.</td>
</tr>
<tr>
  <td>sys_content_provider</td>
  <td>yes</td>
  <td>Identification of the provider that stored the given data into the platform - system field, always necessary - eg. 'jbossorg', 'seam_project' etc.</td>
</tr>
<tr>
  <td>sys_content_type</td>
  <td>yes</td>
  <td>Identifier of the provider defined content type for 'Content Push API'. It is unique in the whole Searchisko so it starts with <code>sys_content_provider</code>, eg. 'jbossorg_jira_issue', 'jbossorg_blog' etc.</td>
</tr>
<tr>
  <td>sys_content_id</td>
  <td>yes</td>
  <td>Content identifier passed in by the provider, it must be unique for the given <code>sys_content_type</code>.</td>
</tr>
<tr>
  <td>sys_visible_for_roles</td>
  <td>false</td>
  <td>Array of strings with user roles of users who can obtain this document over search REST API. 
      User with `admin` role can see document even if not listed there. If field is not present 
      or is empty then document is visible for all users. 
      This setting is additive to the 'Content type level security' setting, you can only tighten visibility there, not to relax it.</td>
</tr>
<tr>
  <td>sys_updated</td>
  <td>yes</td>
  <td>Date of last content update in Searchisko - system field, always necessary, assigned in 'Content Push API'.</td>
</tr>
<tr>
  <td>sys_project</td>
  <td/>
  <td>Normalized Searchisko wide identifier of the project - system field - it is used for the project aggregations and filtering in the Search API.</td>
</tr>
<tr>
  <td>sys_project_name</td>
  <td/>
  <td>Human readable name of project based on <code>sys_project</code> identifier - system field.</td>
</tr>
<tr>
  <td>sys_contributors</td>
  <td/>
  <td>Array of contributing persons, no duplicities in array, persons identifiers normalized during push into Searchisko - each person represented as string <code>Name Surname <primaryemail@email.com></code> - in Search API used for contributors aggregations and filtering.</td>
</tr>
<tr>
  <td>sys_activity_dates</td>
  <td/>
  <td>Array of timestamps (ISO string) representing some activity on the content (when the content was created or changed etc. in source system) - in the Search API used for the time aggregations and filtering.</td>
</tr>
<tr>
  <td>sys_created</td>
  <td/>
  <td>Timestamp (ISO string) representing creation of the content in source system (it's min value from <code>sys_activity_dates</code>), used for sorting on search API.</td>
</tr>
<tr>
  <td>sys_last_activity_date</td>
  <td/>
  <td>Timestamp (ISO string) representing last activity on the content (it's max value from <code>sys_activity_dates</code>), used for sorting on search API.</td>
</tr>
<tr>
  <td>sys_title</td>
  <td/>
  <td>Content title - used to present the document in the basic search GUI results - it can be directly set by the content provider during the push operation.</td>
</tr>
<tr>
  <td>sys_url_view</td>
  <td/>
  <td>URL where the document can be viewed in its original system in human readable form - used to open the document from the basic search GUI - can be directly set by the content provider during the push.</td>
</tr>
<tr>
  <td>sys_description</td>
  <td/>
  <td>Short text representing the content (up to 400 characters) - used to show the content in the basic search GUI results for queries that do not produce highlights - it can be directly set by the content provider during the push, no html formatting.</td>
</tr>
<tr>
  <td>sys_content</td>
  <td/>
  <td>Complete text representing whole content - it can be directly set by the content provider during the push, may contain html formatting. Basic search GUI may use it in search result detail view. Tis field is optional and is used only if this content is useful for some client app.</td>
</tr>
<tr>
  <td>sys_content_content-type</td>
  <td>yes (if <code>sys_content</code> is present)</td>
  <td>MIME identifier of content type stored in the <code>sys_content</code> field eg. <code>text/plain</code>, <code>text/html</code>, <code>text/x-markdown</code>. Must be negotiated with Searchisko Admins so fulltext search analyzer for <code>sys_content</code> is set correctly. Both <code>sys_content</code> and <code>sys_content_content-type</code> are used in <b>Feed API</b> thus they have to be more strictly controlled.</td>
</tr>
<tr>
  <td>sys_content_plaintext</td>
  <td></td>
  <td>This field is expected to contain markup-free full content (same as of `sys_content` field). It makes it easy to run fulltext search API against it and use it for highlighted snippets. Note that it can be populated by pre-processors and <a href="https://github.com/jbossorg/structured-content-tools">structured-content-tools</a> can be used to strip HTML entities.</td>
</tr>
<tr>
  <td>sys_tags</td>
  <td>yes</td>
  <td>Array of tags (Strings) - in the Search API used for aggregations (tag cloud) and filtering - it is not directly pushed by the content provider because we plan a mechanism for additional user defined tags, so we need to rewrite this field internally. The content provider should use <code>tags</code> field instead.</td>
</tr>
<tr>
  <td>tags</td>
  <td/>
  <td>Tags provided by content provider.</td>
</tr>
<tr>
  <td>sys_comments</td>
  <td/>
  <td>Array of comment for issue. <a href="#comment-data-structure-description">'Comment data structure'</a> is described below.</td>
</tr>
<tr>
  <td>sys_rating_avg</td>
  <td/>
  <td>Average rating of Document - system field. It is updated automatically when "Personalized Content Rating API" is used. Contains float number value (with decimal positions) between 1 (worst) and 5 (best). Field is not present if nobody rated document yet.</td>
</tr>
<tr>
  <td>sys_rating_num</td>
  <td/>
  <td>Number of users who rated this Document - system field. It is updated automatically when "Personalized Content Rating API" is used. Contains positive integer number. Field is not present if nobody rated document yet.</td>
</tr>
</tbody>
</table>

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
  <td>Info about Contributor who created comment. <a href="#contributor-data-structure-description">'Contributor data structure'</a> is described below.</td>
</tr>
<tr>
  <td>comment_updater</td>
  <td>Optional. Info about Contributor who updated comment. <a href="#contributor-data-structure-description">'Contributor data structure'</a> is described below.</td>
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
  <td>Normalized Searchisko contributor identifier.</td>
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


### Searchisko Content described by example:

Free JSON Structure representing content. It can be one key - value pair or something more structured.
It's defined only by content provider and must contain mandatory fields defined for given `sys_type`.

	{
	    "tags": ["Content_tag1", "tag2", "tag3"],
	
	    "sys_content_provider": "jbossorg",
	    "sys_content_type": "jbossorg_jira_issue",
	    "sys_content_id": "AS7-1254",
	    "sys_id": "jbossorg_jira_issue-AS7-1254",
	    "sys_type": "issue",
	    "sys_visible_for_roles" : ["developer","employee"],
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
      ],
      "sys_rating_avg" : 4.2,
      "sys_rating_num" : 24
	}

