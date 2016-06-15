Article related to projects
============================================

**sys\_type = "article"**

This data type stored in DCP contains articles published for project - unstructured 
articles like wiki, this type is not intended for project's documentation guides etc.
Contains Articles from [JBoss Developer](https://developer.jboss.org/docs) and 
other wiki systems used by distinct projects. 

## Data structure

### Standard system fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>sys_title</td><td>Getting Started With Mobile RichFaces</td><td>Article title</td></tr>
<tr><td>sys_url_view</td><td>https://developer.jboss.org/wiki/GettingStartedWithMobileRichFaces</td><td>URL of Article view</td></tr>
<tr><td>sys_description</td><td></td><td>Shortened description created from begin of article, contains only clear text</td></tr>
<tr><td>sys_content</td><td></td><td>Full rendered article. May contain HTML tags or some wiki syntax as defined by `sys_content_content-type`.</td></tr>
<tr><td>sys_created</td><td>2013-01-02T06:18:52.000-0500</td><td>Timestamp when article was created, if available</td></tr>
<tr><td>sys_activity_dates</td><td>Dates of activity on this solution, typically dates of creation and last modification.</td></tr>
<tr><td>sys_last_activity_date</td><td>Date of last activity, typically date of last modification if available.</td></tr>
<tr><td>sys_comments</td><td></td><td>Article related comments using 'Comment data structure'</td></tr>
</tbody>
</table>
**Note:** some standard Searchisko system fields prefixed by `sys_` are not described here. Description may be found in general documentation for ["DCP Content object"](dcp_content_object.md).

### Custom fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>modified</td><td>Last modification date, if available.</td></tr>
<tr><td>tags</td><td>["richfaces", "html5", "mobile"]</td><td>Array of tags for article</td></tr>
<tr><td>authors</td><td></td><td>Array of 'Contributor data structure' objects with information about article authors</td></tr>
</tbody>
</table>
