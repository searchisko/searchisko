Article related to projects
============================================

**dcp\_type = "article"**

This data type stored in DCP contains articles published for project - unstructured 
articles like wiki, this type is not intended for project's documentation guides etc.
Contains Articles from [JBoss Community](https://community.jboss.org/docs) and 
other wiki systems used by distinct projects. 

## Data structure

### Standard DCP fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>dcp_title</td><td>Getting Started With Mobile RichFaces</td><td>Article title</td></tr>
<tr><td>dcp_url_view</td><td>https://community.jboss.org/wiki/GettingStartedWithMobileRichFaces</td><td>URL of Article view</td></tr>
<tr><td>dcp_description</td><td></td><td>Shortened description created from begin of article, contains only clear text</td></tr>
<tr><td>dcp_content</td><td></td><td>Full rendered article. May contain HTML tags or some wiki syntax as defined by `dcp_content_content-type`.</td></tr>
<tr><td>dcp_created</td><td>2013-01-02T06:18:52.000-0500</td><td>Timestamp when article was created</td></tr>
<tr><td>dcp_comments</td><td></td><td>Article related comments using 'Comment data structure'</td></tr>
</tbody>
</table>
**Note:** some standard DCP [system fields](dcp_content_object.md) prefixed by `dcp_` are not described here. Description may be found in general documentation for "DCP Content object".

### Custom fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>tags</td><td>["richfaces", "html5", "mobile"]</td><td>Array of tags for article</td></tr>
<tr><td>authors</td><td></td><td>Array of 'Contributor data structure' objects with information about article authors</td></tr>
</tbody>
</table>
