Description of Solution for projects
====================================

**sys\_type = "solution"**

This data type stored in DCP contains information about some common problem and it's solution from knowledge bases, eg https://access.redhat.com/search/browse/solutions.

## Data structure

### Standard system fields
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>sys_type</td><td>Always `solution`</td></tr>
<tr><td>sys_title</td><td>Title of the issue/solution.</td></tr>
<tr><td>sys_description</td><td>Long description text/abstract for the issue solution is for.</td></tr>
<tr><td>sys_created</td><td>Creation date, if available.</td></tr>
<tr><td>sys_activity_dates</td><td>Dates of activity on this solution, typically dates of creation and last modification.</td></tr>
<tr><td>sys_last_activity_date</td><td>Date of last activity, typically date of last modification if available.</td></tr>
</tbody>
</table>
**Note:** some standard Searchisko system fields prefixed by `sys_` are not described here. Description may be found in general documentation for ["Searchisko Content object"](dcp_content_object.md).

### Custom fields
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>modified</td><td>Last modification date, if available.</td></tr>
<tr><td>issue</td><td>Long description text for the issue solution is for.</td></tr>
<tr><td>environment</td><td>Long description text for environment where issue/solution exists.</td></tr>
<tr><td>resolution</td><td>Long description text with resolution for the issue.</td></tr>
<tr><td>root_cause</td><td>Long description text with root cause for the issue.</td></tr>
<tr><td>tags</td><td>Tags from the source knowledgebase system.</td></tr>
</tbody>
</table>

### Example of solution data structure

	{
		"sys_id"                : "rht_knowledgebase_solution-125",
		"sys_type"              : "solution",
		"sys_updated"           : "2013-01-04T09:49Z",
		"sys_created"           : "2011-04-14T11:32Z",
		"sys_content_provider"  : "rht",
		"sys_content_type"      : "rht_knowledgebase_solution",
		"sys_content_id"        : "125",
		"sys_activity_dates"    : ["2011-04-14T11:32Z","2012-11-27T14:22Z"],
		"sys_last_activity_date": "2012-11-27T14:22Z",
		"sys_project"           : "jbosseap",
		"sys_project_name"      : "JBoss Enterprise Application Platform",
		"sys_contributors"      : ["John Doe <john@doe.org>", "Pat Mat <pat@mat.org>"],
		"sys_title"             : "How do I enable SOAP Message Logging for web service calls in JBoss EAP?",
		"sys_url_view"          : "https://access.redhat.com/site/solutions/18678",
		"sys_description"       : "How to enable the logs at HTTP protocol level to debug HTTP errors?",
		"issue"                 : "How to enable the logs at HTTP protocol level to debug HTTP errors?",
		"environment"           : "Red Hat JBoss Enterprise Application Platform (EAP)\r\n  * 4.x - with JBossWS Native only\r\n  * 5.x - with JBossWS Native or CXF\r\n  * 6.x - with JBossWS CXF",
		"resolution"            : "Resolution for this problem is ....",
		"root_cause"            : "Root cause of this problem is ....",
		"modified"              : "2012-11-27T14:22Z",
		"created"               : "2011-04-14T11:32Z",		
		"tags"                  : ["tag1","tag2"],
		"sys_tags"              : ["tag1","tag2"],
		"sys_comments"              : [
		                            {
		                              "comment_id"      : "1254652",
		                              "comment_body"    : "Whole comment text.",
		                              "comment_author"  : {"email_address":"john@doe.org", "display_name":"John Doe", "sys_contributor": "John Doe <john@doe.org>"},
		                              "comment_updater" : {"email_address":"john@doe.org", "display_name":"John Doe", "sys_contributor": "John Doe <john@doe.org>"},
		                              "comment_created" : "2012-11-27T13:55Z",
		                              "comment_updated" : "2012-11-27T14:22Z"
		                            }
		                          ]
	}
