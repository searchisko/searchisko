Issues related to projects
==========================

**dcp\_type = "issue"**

This data type stored in DCP contains informations from project's issue trackers (JIRA, Bugzilla, etc.) about project bugs, feature requests etc.
Contains data from [JBoss Community JIRA](http://issues.jboss.org) and other trackers used by distinct projects. 

## Data structure

Example of issue data structure:

	{
		"dcp_id"                : "jbossorg_jira_issue-ORG-125",
		"dcp_type"              : "issue",
		"dcp_updated"           : "2013-01-04T09:49Z",
		"dcp_content_provider"  : "jbossorg",
		"dcp_content_type"      : "jbossorg_jira_issue",
		"dcp_content_id"        : "ORG-125",
		"dcp_activity_dates"    : ["2011-04-14T11:32Z","2012-11-27T13:55Z","2012-11-27T14:20Z","2012-11-27T14:22Z"],
		"dcp_project"           : "jbossorg",
		"dcp_contributors"      : ["John Doe <john@doe.org>", "Pat Mat <pat@mat.org>"],
		"project_key"           : "ORG",
		"project_name"          : "jboss.org infrastructure",
		"issue_type"            : "Feature Request",
		"dcp_issue_type"        : "Enhancement",
		"dcp_title"             : "Investigate possible theme changes ",
		"dcp_url_view"          : "https://issues.jboss.org/browse/ORG-125",
		"dcp_description"       : "We need to see what we can do with Clearspace L'n'F. As a part of this task some example theme changes should be created and staged. Specifically, some propositions for project pages.",
		"status"                : "Closed",
		"dcp_issue_status"      : "Closed",
		"created"               : "2011-04-14T11:32Z",
		"updated"               : "2012-11-27T14:22Z",
		"resolutiondate"        : "2012-11-27T14:20Z",
		"tags"                  : ["tag1","tag2"],
		"dcp_tags"              : ["tag1","tag2"],
		"reporter"              : {"email_address":"john@doe.org", "display_name":"John Doe", "dcp_contributor": "John Doe <john@doe.org>"},
		"assignee"              : {"email_address":"pat@mat.org", "display_name":"Pat Mat", "dcp_contributor": "Pat Mat <pat@mat.org>"},
		"fix_versions"          : [ {"name":"3.0.0"} ],
		"components"            : [ {"name":"community.jboss.org (SBS)"} ],
		"dcp_comments"              : [
		                            {
		                              "comment_id"      : "1254652",
		                              "comment_body"    : "Whole comment text.",
		                              "comment_author"  : {"email_address":"john@doe.org", "display_name":"John Doe", "dcp_contributor": "John Doe <john@doe.org>"},
		                              "comment_updater" : {"email_address":"john@doe.org", "display_name":"John Doe", "dcp_contributor": "John Doe <john@doe.org>"},
		                              "comment_created" : "2012-11-27T13:55Z",
		                              "comment_updated" : "2012-11-27T14:22Z"
		                            }
		                          ]
	}

Description of data fields for the issue record:
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>dcp_type</td><td>Always `issue`</td></tr>
<tr><td>project_key</td><td>Project key inside of the source issue tracking system, optional. Commonly used for `dcp_project` value lookup during data push.</td></tr>
<tr><td>project_name</td><td>Name of the project from the source issue tracking system</td></tr>
<tr><td>issue_type</td><td>Type of the issue (eg. bug, feature request, etc) value from the source issue tracking system. Used as input for normalization necessary for `dcp_issue_type`.</td></tr>
<tr><td>dcp_issue_type</td><td>Normalized value of the issue type. Possible values: `Bug`, `Enhancement`, `Other`</td></tr>
<tr><td>dcp_title</td><td>Title of the issue.</td></tr>
<tr><td>dcp_description</td><td>Long description text for the issue.</td></tr>
<tr><td>status</td><td>Status of the issue lifecycle/worflow (eg. new, assigned, resolved, closed) from the source issue tracking system. Used as input for normalization necessary for `dcp_issue_status`.</td></tr>
<tr><td>dcp_issue_status</td><td>Normalized value of the issue lifecycle/worflow status. Possible values: `Open`, `In Progress`, `Closed`</td></tr>
<tr><td>created</td><td>Date when the issue was created in the source issue tracking system.</td></tr>
<tr><td>updated</td><td>Date when the issue was last time updated in the source issue tracking system.</td></tr>
<tr><td>resolutiondate</td><td>Date when the issue was resolved in the source issue tracking system</td></tr>
<tr><td>tags</td><td>Tags from the source issue tracking system. (`Labels` field in case of JIRA)</td></tr>
<tr><td>reporter</td><td>Info about contributor who created issue. 'Contributor data structure' is described below.</td></tr>
<tr><td>assignee</td><td>Info about contributor responsible for issue solution. 'Contributor data structure' is described below.</td></tr>
<tr><td>fix_versions</td><td>Array of object describing version where the issue is fixed. Object contains `name` field only for now, with version name.</td></tr>
<tr><td>components</td><td>Array of object describing component of project the issue is for. Object contains `name` field only for now, with component name.</td></tr>
<tr><td>dcp_comments</td><td>Array of comment for issue. 'Comment data structure' is described below.</td></tr>
</tbody>
</table>
**Note:** some standard DCP [system fields](dcp_content_object.md) prefixed by `dcp_` are not described here. Description may be found in general documentation for "DCP Content object".  	

'Contributor data structure' description:
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>email_address</td><td>Optional. Contributor email address from the source issue tracking system. Used to lookup normalized value for `dcp_contributor` field during data push.</td></tr>
<tr><td>display_name</td><td>Optional. Contributor display name from the source issue tracking system.</td></tr>
<tr><td>dcp_contributor</td><td>Normalized DCP contributor identifier.</td></tr>
</tbody>
</table>

'Comment data structure' description:
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>comment_id</td><td>Identifier of the comment from source system. Optional.</td></tr>
<tr><td>comment_body</td><td>Text of the comment</td></tr>
<tr><td>comment_author</td><td>Info about contributor who created comment. 'Contributor data structure' is described above.</td></tr>
<tr><td>comment_updater</td><td>Info about contributor who updated comment. 'Contributor data structure' is described above.</td></tr>
<tr><td>comment_created</td><td>Date when the comment was created in the source issue tracking system.</td></tr>
<tr><td>comment_updated</td><td>Date when the comment was updated in the source issue tracking system.</td></tr>
</tbody>
</table>
