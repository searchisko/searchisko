Documentation page related to projects
======================================

**sys\_type = "documentation"**

This data type stored in DCP contains information about one page (webpage accessible over URL) of the project's documentation.  

## Data structure

### Standard system fields
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>sys_type</td><td>Always `documentation`</td></tr>
<tr><td>sys_title</td><td>Title of the documentation page.</td></tr>
<tr><td>sys_url_view</td><td>URL of documentation page</td></tr>
<tr><td>sys_description</td><td>Short description text for the documentation page.</td></tr>
<tr><td>sys_content_plaintext</td><td>Whole plain text content of documentation page to be used for fulltext search</td></tr>
<tr><td>sys_content</td><td>Is not used typically as we do not need this full content for any client app.</td></tr>
</tbody>
</table>
**Note:** some standard Searchisko system fields prefixed by `sys_` are not described here. Description may be found in general documentation for ["DCP Content object"](dcp_content_object.md).

### Custom fields
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>documentation_type</td><td>Type of the documentation page belongs to, eg. `user`, `reference`, `admin` </td></tr>
<tr><td>release_version</td><td>Release version of the project this documentation page belongs to, eg. `1.0.0`, `latest` etc</td></tr>
</tbody>
</table>

### Example of issue data structure

	{
		"sys_id"                : "cdi-weld_weld_documentation-sdad125",
		"sys_type"              : "documentation",
		"sys_updated"           : "2016-02-16T09:49Z",
		"sys_content_provider"  : "cdi-weld",
		"sys_content_type"      : "cdi-weld_weld_documentation",
		"sys_content_id"        : "sdad125",
		"sys_project"           : "weld",
		"sys_project_name"      : "Weld",
		"documentation_type"    : "reference",
		"release_version"       : "latest",
		"sys_title"             : "Part I. Beans",
		"sys_url_view"          : "http://docs.jboss.org/weld/reference/latest/en-US/html/part-1.html",
		"sys_description"       : "Weld CDI Reference Implementation - Reference Documentation - Part I. Beans",
		"sys_content_plaintext" : "The CDI specification defines a set of complementary services that help improve the structure of application code. CDI layers an enhanced lifecycle and interaction model over existing ..."
	}
