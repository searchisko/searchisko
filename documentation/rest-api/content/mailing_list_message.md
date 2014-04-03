Mailing list messages related to projects
=========================================

**sys\_type = "mailing_list_message"**

This data contains messages from mailing list. In out case we have public mailing list <https://lists.jboss.org/mailman/listinfo/>
and we parse and index the data from internal Mailman archives (see [mbox_integration](https://github.com/searchisko/mbox_tools) for source code).

## Data structure

### Standard system fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>sys_content_id</td><td></td><td>${message_id}</td></tr>
<tr><td>sys_project</td><td></td><td>Derived from ${project} field.</td></tr>
<tr><td>sys_project_name</td><td></td><td>Derived from ${project} field.</td></tr>
<tr><td>sys_url_view</td><td></td><td>URL where this message can be accessed. Needs to be provided. Typically this is URL to mailman archive entry.</td></tr>
<tr><td>sys_title</td><td></td><td>${subject}</td></tr>
<tr><td>sys_description</td><td></td><td>${message_snippet}</td></tr>
<tr><td>sys_content</td><td></td><td>Derived from <code>*text/html_message(s)</code>.</td></tr>
<tr><td>sys_content_content-type</td><td></td><td>Relevant value for <code>sys_content</code>.</td></tr>
<tr><td>sys_contributors</td><td></td><td>Derived from ${author} field.</td></tr>
<tr><td>sys_created</td><td></td><td>${date}</td></tr>
<tr><td>sys_activity_dates</td><td></td><td>Derived from ${date} field.</td></tr>
<tr><td>sys_last_activity_date</td><td></td><td>Derived from ${date} field.</td></tr>
<tr><td>sys_tags</td><td></td><td>Provided by client. Typically this is populated by the category of mail list. E.g.: <code>dev</code>, <code>users</code>, <code>announce</code></td></tr>
</tbody>
</table>
**Note:** some standard Searchisko system fields prefixed by `sys_` are not described here.
Description may be found in general documentation for ["Searchisko Content object"](dcp_content_object.md).

### Custom fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>author_name</td><td>Pete Muir</td><td>Originator of the message, just the name part. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.2">3.6.2. Originator fields</a>.</td></tr>
<tr><td>author_email</td><td>&lt;pmuir@redhat.com&gt;</td><td>Originator of the message, just the email part. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.2">3.6.2. Originator fields</a>.</td></tr>
<tr><td>to</td><td></td><td>Value of recipient field <code>To</code>. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.3">3.6.3. Destination address fields</a></td></tr>
<tr><td>message_id_original</td><td></td><td>Original value of <code>Message-Id</code> field. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.4">3.6.4. Identification fields</a>. Note that message can be sent to many mailing lists so this value may not be unique across document collection.</td></tr>
<tr><td>message_id</td><td></td><td>${message_id_original} with optional suffix. This should be unique identifier of the message across whole document collection.</td></tr>
<tr><td>references</td><td></td><td>Content of <code>References</code> field. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.4">3.6.4. Identification fields</a>.</td></tr>
<tr><td>subject_original</td><td></td><td>Content of <code>Subject</code> field. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.5">3.6.5. Informational fields</a>.</td></tr>
<tr><td>subject</td><td></td><td>Cleared value of ${subject_original} field (removed 'RE:' and similar tokens).</td></tr>
<tr><td>date</td><td></td><td>Value of <code>Date</code> field. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.1">3.6.1. The origination date field</a>.</td></tr>
<tr>
  <td>message_snippet</td><td></td>
  <td>A short snippet from message body. The content can be taken from <code>first_text_message_without_quotes</code>, <code>first_text_message</code> or <code>first_html_message</code> (in this order).</td>
</tr>
<tr><td>first_text_message</td><td></td><td>Content of the first <code>text/plain</code> message.</td></tr>
<tr><td>first_text_message_without_quotes</td><td></td><td>Similar to <code>first_text_message</code> but quotes are stripped out.</td></tr>
<tr><td>first_html_message</td><td></td><td>First <code>test/html</code> message from the body. Populated <b>only iff</b> <code>first_test_message</code> is not available.</td></tr>
<tr><td>text_messages</td><td></td><td>If the body contains other <code>text/plain</code> messages than the <code>first_text_message</code> then they are stored in array here.</td></tr>
<tr><td>text_messages_cnt</td><td></td><td>Number of items in <code>text_messages</code>.</td></tr>
<tr><td>html_messages</td><td></td><td>If the body contains other <code>text/html</code> messages than the <code>first_text_message</code> then they are stored in array here.</td></tr>
<tr><td>html_messages_cnt</td><td></td><td>Number of items in <code>html_messages</code>.</td></tr>
<tr><td>message_attachments</td><td></td><td>Array of parsed attachments. See <a href="#message-attachment-fields">Message attachment fields</a> for details.</td></tr>
<tr><td>message_attachments_cnt</td><td></td><td>Number of parsed attachments.</td></tr>
</tbody>
</table>

#### Message attachment fields

<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>content_type</td><td></td><td></td></tr>
<tr><td>filename</td><td></td><td></td></tr>
<tr><td>content</td><td></td><td>Extracted plain text content from the attachment.</td></tr>
</tbody>
</table>

### Example of mailing list message data structure

	{
		"sys_id" : "Provided by Searchisko",
		"sys_type" : "Provided by Searchisko",
		"sys_content_type" : "Provided by Searchisko",
		"sys_content_id" : "Provided by Searchisko",
		"sys_project" : "Extracted from `sys_projects` by Searchisko preprocessor.",
		"sys_project_name" : "Extracted from `sys_projects` by Searchisko preprocessor.",
		"sys_url_view" : "http://lists.jboss.org/pipermail/cdi-dev/2012-October/003128.html",
		"sys_content_content-type" : "`text/plain` or `text/html`",
		"sys_title" : "${subject}",
		"sys_content" : "FYI, I submitted the PRD. I'll push the api to maven central when the JCP confirm the PRD is posted. Begin forwarded message:",
		"sys_created" : "${date}",
		"sys_activity_dates" : "[${date}]",
		"sys_last_activity_date" : "${date}",
		"sys_description" : "${message_snippet}",
		"sys_tags" : "dev",
		"sys_contributors" : "Pete Muir <pmuir@redhat.com>",

    	"author_name" : "Pete Muir",
    	"author_email" : "pmuir@redhat.com",
    	"to" : [ "cdi-dev <cdi-dev@lists.jboss.org>" ],
    	"subject_original" : "[cdi-dev] Fwd: JSR-346 Public Review Draft",
    	"subject" : "JSR-346 Public Review Draft",
    	"date" : "2012-10-26T17:32:33.000Z",
    	"message_id_original" : "<C763827E-21C3-4471-BCCC-089F2F5A70FC@redhat.com>",
    	"message_id" : "<C763827E-21C3-4471-BCCC-089F2F5A70FC@redhat.com>",
    	"references" : [ "<BA0252D3-19B1-40E8-85D0-A72F0C3B5225@redhat.com>" ],
    	"message_snippet" : "FYI, I submitted the PRD. I'll push the api to maven central when the JCP confirm the PRD is posted. Begin forwarded message",
    	"first_text_message" : "FYI, I submitted the PRD.\n\nI'll push the api to maven central when the JCP confirm the PRD is posted.\n\n\nBegin forwarded message:\n\n> From: Pete Muir <pmuir@redhat.com>\n> Subject: JSR-346 Public Review Draft \n> Date: 26 October 2012 18:31:09 GMT+01:00\n> To: spec-submit@jcp.org\n> \n",
    	"first_text_message_without_quotes" : "FYI, I submitted the PRD. I'll push the api to maven central when the JCP confirm the PRD is posted. Begin forwarded message:",
    	"text_messages" : [ ">\r\n>\r\n> 1) Please see attached\r\n> 2) 30 days\r\n> 3)\r\n>\r\n> A. Does the specification include software codes\r\n> in the following format:\r\n> Binary : No\r\n> Source (compilable) : No\r\n> Javadocs : Yes\r\n> B. Do the codes or the spec call on, contain, use\r\n> or demonstrate encryption technology? No\r\n>\r\n> 4. Licensing is unchanged\r\n> 5. 12 February 2013\r\n> 6. Version: 1.1, Release Date: 15 March 2013, Spec Lead: Red Hat, Inc. 1801 Varsity Drive, Raleigh, North Carolina 27606. United States.\r\n> 7.\r\n>\r\n> \t• The public can read the names of the people on the Expert Group (ie, not just JCP Members)\r\n>\r\n> http://jcp.org/en/jsr/detail?id46\r\n>\r\n> \t• The Expert Group business is regularly reported on a publicly readable alias.\r\n>\r\n> http://lists.jboss.org/pipermail/cdi-dev/\r\n>\r\n> \t• The schedule for the JSR is publicly available, it's current, and I update it regularly.\r\n>\r\n> https://github.com/jboss/cdi/wiki\r\n>\r\n> \t• The public can read/write to a wiki for my JSR.\r\n>\r\n> https://github.com/jboss/cdi/wiki\r\n>\r\n> \t• I have a discussion board for my JSR and I regularly read and respond to posts on that board.\r\n>\r\n> http://lists.jboss.org/pipermail/cdi-dev/\r\n>\r\n>\r\n> \t• There is an issue-tracker for my JSR that the public can read.\r\n>\r\n> http://issues.jboss.org/browse/CDI\r\n>\r\n> \t• I have spoken at conferences and events about my JSR recently.\r\n>\r\n> Yes, e.g JavaOne\r\n>\r\n> \t• I am using open-source processes for the development of the RI and/or TCK.\r\n>\r\n> Yes. https://github.com/weld & https://github.com/jboss/cdi-tck\r\n>\r\n> \t• The Community tab for my JSR has links to and information about all public communication mechanisms and sites for the development of my JSR.\r\n>\r\n> Yes\r\n\r\n" ],
        "text_messages_cnt" : 1,
        "html_messages_cnt" : 0,

		"message_attachments" : [ {
			"content_type" : "application/pdf",
			"filename" : "cdi-spec.pdf",
			"content" : "Extracted content of this attachment in plain text. All formatting and images are dropped."
		}, {
			"content_type" : "application/zip",
			"filename" : "cdi-api-1.1-PRD.zip",
			"content" : "Extracted content of this attachment in plain text. All formatting and images are dropped."
		} ],
		"message_attachments_cnt" : 2
	}