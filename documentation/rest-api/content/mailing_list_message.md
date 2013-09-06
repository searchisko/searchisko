Mailing list messages related to projects
=========================================

**sys\_type = "mailing_list_message"**

This data contains messages from mailing list. In out case we have public mailing list <https://lists.jboss.org/mailman/listinfo/>
and we parse and index the data from internal Mailman archives (see [mbox_integration](https://github.com/searchisko/mbox_integration) for source code).

## Data structure

### Standard DCP fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>sys_id</td><td>TBD</td><td>TBD</td></tr>
<tr><td>sys_type</td><td>TBD</td><td>TBD</td></tr>
<tr><td>sys_content_type</td><td>TBD</td><td>TBD</td></tr>
<tr><td>sys_content_id</td><td>TBD</td><td>TBD</td></tr>
<tr><td>sys_url_view</td><td>TBD</td><td>TBD</td></tr>
</tbody>
</table>
**Note:** some standard DCP [system fields](dcp_content_object.md) prefixed by `sys_` are not described here.
Description may be found in general documentation for "DCP Content object".

### Custom fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>author</td><td>Pete Muir <pmuir@redhat.com></td><td>Originator of the message. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.2">3.6.2. Originator fields</a>.</td></tr>
<tr><td>to</td><td></td><td>Value of recipient field <code>To</code>. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.3">3.6.3. Destination address fields</a></td></tr>
<tr><td>message_id_original</td><td></td><td>Original value of <code>Message-Id</code> field. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.4">3.6.4. Identification fields</a>. Note that message can be sent to many mailing lists so this value may not be unique across document collection.</td></tr>
<tr><td>message_id</td><td></td><td>${message_id_original} with optional suffix. This should be unique identifier of the message across whole document collection.</td></tr>
<tr><td>references</td><td></td><td>Content of <code>References</code> field. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.4">3.6.4. Identification fields</a>.</td></tr>
<tr><td>subject_original</td><td></td><td>Content of <code>Subject</code> field. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.5">3.6.5. Informational fields</a>.</td></tr>
<tr><td>subject</td><td></td><td>Cleared value of ${subject_original} field (removed 'RE:' and similar tokens).</td></tr>
<tr><td>date</td><td></td><td>Value of <code>Date</code> field. See <a href="http://tools.ietf.org/html/rfc2822#section-3.6.1">3.6.1. The origination date field</a>.</td></tr>
<tr><td>message_snippet</td><td></td><td>A short snippet from message body.</td></tr>
<tr><td>first_text_message</td><td></td><td></td></tr>
<tr><td>first_text_message_without_quotes</td><td></td><td></td></tr>
<tr><td>text_messages</td><td></td><td></td></tr>
<tr><td>text_messages_cnt</td><td></td><td></td></tr>
<tr><td>html_messages</td><td></td><td></td></tr>
<tr><td>html_messages_cnt</td><td></td><td></td></tr>
<tr><td>message_attachments</td><td></td><td>TBD</td></tr>
<tr><td>message_attachments_cnt</td><td></td><td></td></tr>
</tbody>
</table>

<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>content_type</td><td></td><td></td></tr>
<tr><td>filename</td><td></td><td></td></tr>
<tr><td>content</td><td></td><td></td></tr>
</tbody>
</table>

### Example of mailing list message data structure:

    {
        "sys_id" : "",
        "sys_type" : "",
        "sys_content_type" : "",
        "sys_content_id" : "",
        "sys_url_view" : "http://lists.jboss.org/pipermail/cdi-dev/2012-October/003128.html",
        "sys_title" : "${subject}",
        "sys_content" : "?",
        "sys_created" : "${date}",
        "sys_description" : "${message_snippet}",

        "author" : "Pete Muir <pmuir@redhat.com>",
        "to" : [ "cdi-dev <cdi-dev@lists.jboss.org>" ],
        "subject_original" : "[cdi-dev] Fwd: JSR-346 Public Review Draft",
        "subject" : "JSR-346 Public Review Draft",
        "date" : "2012-10-26T17:32:33.000Z",
        "message_id_original" : "<C763827E-21C3-4471-BCCC-089F2F5A70FC@redhat.com>",
        "message_id" : "<C763827E-21C3-4471-BCCC-089F2F5A70FC@redhat.com>",
        "references" : [ "<BA0252D3-19B1-40E8-85D0-A72F0C3B5225@redhat.com>" ],
        "message_snippet" : "FYI, I submitted the PRD. I'll push the api to maven central when the JCP confirm the PRD is posted. Begin forwarded message",
        "first_text_message" : "FYI, I submitted the PRD.\n\nI'll push the api to maven central when the JCP confirm the PRD is posted.\n\n\nBegin forwarded message:\n\n&gt; From: Pete Muir &lt;pmuir@redhat.com&gt;\n&gt; Subject: JSR-346 Public Review Draft \n&gt; Date: 26 October 2012 18:31:09 GMT+01:00\n&gt; To: spec-submit@jcp.org\n&gt; \n",
        "first_text_message_without_quotes" : "FYI, I submitted the PRD. I'll push the api to maven central when the JCP confirm the PRD is posted. Begin forwarded message:",
        "text_messages" : [ "&gt;\r\n&gt;\r\n&gt; 1) Please see attached\r\n&gt; 2) 30 days\r\n&gt; 3)\r\n&gt;\r\n&gt; A. Does the specification include software codes\r\n&gt; in the following format:\r\n&gt; Binary : No\r\n&gt; Source (compilable) : No\r\n&gt; Javadocs : Yes\r\n&gt; B. Do the codes or the spec call on, contain, use\r\n&gt; or demonstrate encryption technology? No\r\n&gt;\r\n&gt; 4. Licensing is unchanged\r\n&gt; 5. 12 February 2013\r\n&gt; 6. Version: 1.1, Release Date: 15 March 2013, Spec Lead: Red Hat, Inc. 1801 Varsity Drive, Raleigh, North Carolina 27606. United States.\r\n&gt; 7.\r\n&gt;\r\n&gt; \t• The public can read the names of the people on the Expert Group (ie, not just JCP Members)\r\n&gt;\r\n&gt; http://jcp.org/en/jsr/detail?id46\r\n&gt;\r\n&gt; \t• The Expert Group business is regularly reported on a publicly readable alias.\r\n&gt;\r\n&gt; http://lists.jboss.org/pipermail/cdi-dev/\r\n&gt;\r\n&gt; \t• The schedule for the JSR is publicly available, it's current, and I update it regularly.\r\n&gt;\r\n&gt; https://github.com/jboss/cdi/wiki\r\n&gt;\r\n&gt; \t• The public can read/write to a wiki for my JSR.\r\n&gt;\r\n&gt; https://github.com/jboss/cdi/wiki\r\n&gt;\r\n&gt; \t• I have a discussion board for my JSR and I regularly read and respond to posts on that board.\r\n&gt;\r\n&gt; http://lists.jboss.org/pipermail/cdi-dev/\r\n&gt;\r\n&gt;\r\n&gt; \t• There is an issue-tracker for my JSR that the public can read.\r\n&gt;\r\n&gt; http://issues.jboss.org/browse/CDI\r\n&gt;\r\n&gt; \t• I have spoken at conferences and events about my JSR recently.\r\n&gt;\r\n&gt; Yes, e.g JavaOne\r\n&gt;\r\n&gt; \t• I am using open-source processes for the development of the RI and/or TCK.\r\n&gt;\r\n&gt; Yes. https://github.com/weld & https://github.com/jboss/cdi-tck\r\n&gt;\r\n&gt; \t• The Community tab for my JSR has links to and information about all public communication mechanisms and sites for the development of my JSR.\r\n&gt;\r\n&gt; Yes\r\n\r\n" ],
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