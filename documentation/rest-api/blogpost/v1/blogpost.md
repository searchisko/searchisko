Blog posts
==========

**dcp\_type = "blogpost"**

This data type stored in DCP contains blog posts aggregated from project related blogs. 
It's mainly related to the [JBoss Community Planet](http://planet.jboss.org) blog aggregating service.

## Data structure

### Standard DCP fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>dcp_title</td><td>How to Migrate Your Heroku Ruby Application to OpenShift</td><td>Blog post title</td></tr>
<tr><td>dcp_url_view</td><td>https://openshift.redhat.com/community/blogs/heroku-ruby-app-migration</td><td>URL of blog post</td></tr>
<tr><td>dcp_description</td><td></td><td>Shorted description containing only clear text</td></tr>
</tbody>
</table>


### Custom fields
<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td>author</td><td></td><td>Full name of author</td></tr>
<tr><td>avatar_link</td><td>https://community.jboss.org/people/sbs-default-avatar/avatar/46.png</td><td>URL of avatar</td></tr>
<tr><td>published</td><td>2013-01-02T06:18:52.000-0500</td><td>Timestamp when blog post was published</td></tr>
<tr><td>modified</td><td>2013-01-02T06:18:52.000-0500</td><td>Timestamp when blog post was modified. Can be same as published</td></tr>
<tr><td>content</td><td></td><td>Full rendered blog post. Can contains HTML tags</td></tr>
<tr><td>tags</td><td>["ruby 1.9", "ruby", "postgresql"]</td><td>Array of tags in original blog posts</td></tr>
</tbody>
</table>

