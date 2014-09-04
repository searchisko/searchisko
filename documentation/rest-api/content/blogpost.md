Blog posts
==========

**sys\_type = "blogpost"**

This data type stored in DCP contains blog posts aggregated from project related blogs. 
It's mainly related to the [JBoss Community Planet](http://planet.jboss.org) blog aggregating service.

## Data structure

### Standard system fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>sys_title</td><td>How to Migrate Your Heroku Ruby Application to OpenShift</td><td>Blog post title</td></tr>
<tr><td>sys_url_view</td><td>https://openshift.redhat.com/community/blogs/heroku-ruby-app-migration</td><td>URL of blog post</td></tr>
<tr><td>sys_description</td><td></td><td>Shorted description containing only clear text</td></tr>
<tr><td>sys_content</td><td></td><td>Full rendered blog post. Can contain HTML tags</td></tr>
<tr><td>sys_created</td><td>2013-01-02T06:18:52.000-0500</td><td>Timestamp when blog post was published</td></tr>
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
<tr><td>author</td><td></td><td>Full name of author</td></tr>
<tr><td>avatar_link</td><td>https://developer.jboss.org/people/sbs-default-avatar/avatar/46.png</td><td>URL of avatar</td></tr>
<tr><td>modified</td><td>2013-01-02T06:18:52.000-0500</td><td>Timestamp when blog post was modified. Can be same as `sys_created`</td></tr>
<tr><td>tags</td><td>["ruby 1.9", "ruby", "postgresql"]</td><td>Array of tags in original blog posts</td></tr>
</tbody>
</table>

