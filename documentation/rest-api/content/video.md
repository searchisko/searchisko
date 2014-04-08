Article related to projects
============================================

**sys\_type = "video"**

This data type contains info about videos published for project. Source of videos can be services like vimeo.com, youtube.com etc.

## Data structure

### Standard system fields
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>sys_title</td><td>Article title</td></tr>
<tr><td>sys_url_view</td><td>URL of page where video can be shown</td></tr>
<tr><td>sys_description</td><td>Text description of video</td></tr>
<tr><td>sys_created</td><td>Timestamp when video was created</td></tr>
</tbody>
</table>
**Note:** some standard Searchisko system fields prefixed by `sys_` are not described here as they are filled automatically during data push. Description may be found in general documentation for ["DCP Content object"](dcp_content_object.md).

### Custom fields
<table border="1">
<thead>
  <th>Field</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>duration</td><td>Duration of video in seconds. Integer number.</td></tr>
<tr><td>thumbnail</td><td>URL of video thumbnail image</td></tr>
<tr><td>tags</td><td>Array of tags for video</td></tr>
<tr><td>author</td><td>identifier of author from source system (eg. vimeo username)</td></tr>
<tr><td>contributors</td><td>Array of identifiers of contributors from source system (eg. vimeo usernames)</td></tr>
</tbody>
</table>

### Example of pushed data

````
{
    "sys_title": "5 ways to deploy your application to JBoss AS 7",
    "sys_description": "In this screencast we show you 5 ways to deploy your application to JBoss AS 7.",
    "sys_url_view": "http://www.jboss.org/video/vimeo/25831010",
    "author": "jdoe",
    "contributors": [ "jdoe", "pmat"],
    "sys_created": "2011-06-30T14:57:47+00:00",
    "sys_last_activity_date": "2014-04-01T03:06:26+00:00",
    "duration": 351,
    "thumbnail": "http://b.vimeocdn.com/ts/170/577/170577275_200.jpg",
    "tags": [ "jbossas7", "javaee", "jboss"]
}
````

### Example of final data stored in Searchisko

````
{
    "sys_id"                : "jbossdeveloper_vimeo-25831010",
    "sys_type"              : "video",
    "sys_updated"           : "2013-01-04T09:49Z",
    "sys_content_provider"  : "jbossdeveloper",
    "sys_content_type"      : "jbossdeveloper_vimeo",
    "sys_content_id"        : "25831010",
    "sys_title"             : "5 ways to deploy your application to JBoss AS 7",
    "sys_description"       : "In this screencast we show you 5 ways to deploy your application to JBoss AS 7.",
    "sys_url_view"          : "http://www.jboss.org/video/vimeo/25831010",
    "author"                : "jdoe",
    "contributors"          : [ "jdoe", "pmat"],
    "sys_contributors"      : ["John Doe <john@doe.org>", "Pat Mat <pat@mat.org>"],
    "sys_created"           : "2011-06-30T14:57:47+00:00",
    "sys_last_activity_date": "2014-04-01T03:06:26+00:00",
    "sys_activity_dates"    : ["2011-06-30T14:57:47+00:00","2014-04-01T03:06:26+00:00"],
    "duration": 351,
    "thumbnail": "http://b.vimeocdn.com/ts/170/577/170577275_200.jpg",
    "tags": [ "jbossas7", "javaee", "jboss"],
    "sys_tags": [ "jbossas7", "javaee", "jboss"]
}
````