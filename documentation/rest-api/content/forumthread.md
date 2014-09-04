Discussion forum threads related to projects
============================================

**sys\_type = "forumthread"**

This data type stored in Searchisko contains information from project's discussion forums.
DCP contains data from [JBoss Developer Forum](http://developer.jboss.org/threads) and other systems used by distinct projects. 

## Data structure

### Standard system fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>sys_title</td><td>Load image from outside the web context in JBoss AS 7</td><td>Thread title</td></tr>
<tr><td>sys_url_view</td><td>https://developer.jboss.org/thread/227893</td><td>URL of forum thread view</td></tr>
<tr><td>sys_description</td><td></td><td>Shorted description created from forum question, containing only clear text</td></tr>
<tr><td>sys_content</td><td></td><td>Full rendered forum question. May contain HTML tags or some wiki syntax as defined by `sys_content_content-type`.</td></tr>
<tr><td>sys_created</td><td>2013-01-02T06:18:52.000-0500</td><td>Timestamp when thread was started</td></tr>
<tr><td>sys_comments</td><td></td><td>All replies to the forum question are stored as comments using 'Comment data structure'</td></tr>
</tbody>
</table>
**Note:** some standard Searchisko system fields prefixed by `sys_` are not described here. Description may be found in general documentation for ["Searchisko Content object"](dcp_content_object.md).

### Custom fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>tags</td><td>["ruby 1.9", "ruby", "postgresql"]</td><td>Array of tags in original forum thread</td></tr>
<tr><td>authors</td><td></td><td>Array of 'Contributor data structure' objects with information about thread question author</td></tr>
</tbody>
</table>
