Web pages
=========

**sys\_type = "webpage"**

This data type contains info about web page.

## Data structure

### Standard system fields
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>sys_type</td><td>Always `webpage`</td></tr>
<tr><td>sys_title</td><td>Title of web page</td></tr>
<tr><td>sys_url_view</td><td>URL of web page</td></tr>
<tr><td>sys_description</td><td>Short text description of web page</td></tr>
<tr><td>sys_content_plaintext</td><td>Whole plain text content of web page to be used for fulltext search</td></tr>
<tr><td>sys_content</td><td>Is not used typically as we do not need this full content for any client app.</td></tr>
</tbody>
</table>
**Note:** some standard Searchisko system fields prefixed by `sys_` are not described here as they are filled automatically during data push. Description may be found in general documentation for ["DCP Content object"](dcp_content_object.md).
