Addon/Plugin related to project
===============================

**sys\_type = "addon"**

This data type stored in DCP contains information from project's addons/plugins. 

## Data structure

### Standard system fields
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>sys_type</td><td>Always `addon`</td></tr>
<tr><td>sys_title</td><td>Title/name of the Addon.</td></tr>
<tr><td>sys_description</td><td>Description text for the addon.</td></tr>
<tr><td>sys_content</td><td>Long description/documentation text for the addon to be used for fulltext search.</td></tr>
<tr><td>sys_created</td><td>Date when the addon was created.</td></tr>
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
<tr><td>sys_url_doc</td><td>URL of Addon documentation</td></tr>
<tr><td>sys_license</td><td>Addon's license acronym (eg. `EPL`, `ASL`, `LGPL` etc.)</td></tr>
</tbody>
</table>

### Example of Addon data structure pushed into Searchisko

	{
		"sys_title"             : "Addon for sitemap",
		"sys_url_view"          : "http://jboss.org/addon/sitemap",
		"sys_description"       : "This addon allows to generate sitemap",
		"sys_license"           : "ASL",
		"sys_url_doc"           : "http://jboss.org/addon/sitemap/doc",
		"sys_created"           : "2011-04-14T11:32Z",
		"sys_activity_dates"    : ["2011-04-14T11:32Z","2012-11-27T13:55Z","2012-11-27T14:20Z","2012-11-27T14:22Z"],
		"tags"                  : ["tag1","tag2"]
	}

### Example of Addon data structure obtained from Searchisko

	{
		"sys_id"                : "jbossorg_addon-125",
		"sys_type"              : "addon",
		"sys_updated"           : "2013-01-04T09:49Z",
		"sys_content_provider"  : "jbossorg",
		"sys_content_type"      : "jbossorg_addon",
		"sys_content_id"        : "125",
		"sys_activity_dates"    : ["2011-04-14T11:32Z","2012-11-27T13:55Z","2012-11-27T14:20Z","2012-11-27T14:22Z"],
		"sys_project"           : "jbossorg",
		"sys_project_name"      : "jboss.org website",
		"sys_contributors"      : ["John Doe <john@doe.org>", "Pat Mat <pat@mat.org>"],
		"sys_title"             : "Addon for sitemap",
		"sys_url_view"          : "http://jboss.org/addon/sitemap",
		"sys_description"       : "This addon allows to generate sitemap",
		"sys_license"           : "ASL",
		"sys_url_doc"           : "http://jboss.org/addon/sitemap/doc",
		"sys_created"           : "2011-04-14T11:32Z",
		"tags"                  : ["tag1","tag2"],
		"sys_tags"              : ["tag1","tag2"]
	}
