Projects search API
===================

**sys\_type = "project\_info"**

A human readable view for the data you can ask is [here](http://www.jboss.org/projects).

This *sys_type* was created a result of this [feature request](https://issues.jboss.org/browse/ORG-1446 "Expose Project information as a HTTP API"). In the beginning Magnolia will submit all the data to DCP, but later some projects may be granted the permission to do the same thing for themselves.

Also Magnolia will supply the results for the data before the DCP will be fully implemented. Both Magnolia and DCP (after it is implemented) will return the JSON structure as described in this article. After DCP is ready, the content will be served from it and the feature will be turned off on Magnolia. After the change each application that will take the data from Magnolia will just change the URL to DCP format.

For now the data will be available on http(s)://www.jboss.org/rest/projectData. If you specify the *sys_content_id* in the URL, i.e. https://www.jboss.org/rest/projectData/gatein, you will receive the same structure, but with just one record.


An example of the search results:
---------------------------------

	{
	   "hits":{
	      "total":2,
	      "hits":[
	         {
	            "_source":{
	               "sys_type":"project_info",
	               "sys_content_provider":"jbossorg",
	               "sys_content_type":"jbossorg_project_info",
	               "sys_id":"jbossorg_project_info-gatein",
	               "sys_content_id":"gatein",
	               "sys_title":"GateIn",
	               "sys_url_view":"/gatein",
	               "sys_description":"",
	               "sys_updated":"2013-01-04T09:49Z",
	               "communityLink":"http://developer.jboss.org/en/gatein",
	               "jiraLink":"https://jira.jboss.org/jira/browse/GTNPORTAL",
	               "otherLicenseLink":"",
	               "downloadsLink_UUID":"aa917c95-c909-4a0c-aa25-66103c7c35a5",
	               "levels":"10",
	               "start":"/gatein",
	               "devForumLink":"http://developer.jboss.org/en/gatein/dev?view=discussions",
	               "githubLink":"https://github.com/gatein",
	               "hudsonLink":"https://hudson.jboss.org/hudson/view/GateIn/",
	               "blogLink":"http://developer.jboss.org/en/gatein?view=blog",
	               "docsLink":"/gatein/documentation",
	               "excludeInProjectMatrix":"false",
	               "projectName":"GateIn",
	               "start_UUID":"cda044f6-ed55-4cfa-a571-41d421ad655a",
	               "downloadsLink":"/gatein/downloads",
	               "nodePath":"/gatein",
	               "chatLink":"irc://irc.freenode.net/gatein",
	               "knowledgeBaseLink":"https://developer.jboss.org/en/gatein?view=documents",
	               "twitterLink":"http://www.twitter.com/gatein",
	               "docsLink_UUID":"882ca234-1ff3-42d0-898a-aa797f61335d",
	               "archived":"false",
	               "license":"LGPL",
	               "userForumLink":"http://developer.jboss.org/en/gatein?view=discussions"
	            }
	         },
	         {
	            "_source":{
	               "sys_type":"project_info",
	               "sys_content_provider":"jbossorg",
	               "sys_content_type":"jbossorg_project_info",
	               "sys_id":"jbossorg_project_info-gatein_portletcontainer",
	               "sys_content_id":"gatein_portletcontainer",
	               "sys_title":"GateIn Portlet Container",
	               "sys_url_view":"/gatein/portletcontainer",
	               "sys_description":"",
	               "sys_updated":"2013-01-04T09:49Z",
	               "jiraLink":"https://jira.jboss.org/jira/browse/GTNPC",
	               "otherLicenseLink":"",
	               "anonymousLink":"http://anonsvn.jboss.org/repos/gatein/components/pc/",
	               "levels":"2",
	               "downloadsLink_UUID":"dedb6432-aa7b-4340-b529-20bca0defa70",
	               "start":"/gatein/portletcontainer",
	               "docsLink":"/gatein/portletcontainer/documentation",
	               "fisheyeLink":"http://fisheye.jboss.org/browse/gatein/components/pc",
	               "excludeInProjectMatrix":"false",
	               "projectName":"GateIn Portlet Container",
	               "start_UUID":"cbabac62-74e1-464f-9cc0-16bd96154d11",
	               "downloadsLink":"/gatein/portletcontainer/downloads",
	               "nodePath":"/gatein/portletcontainer",
	               "viewvcLink":"http://viewvc.jboss.org/cgi-bin/viewvc.cgi/gatein/components/pc/",
	               "knowledgeBaseLink":"http://www.jboss.org/community/wiki/JBossPortletContainer",
	               "archived":"false",
	               "docsLink_UUID":"0a7d07b4-81fb-4af8-a539-fdae51d075aa",
	               "license":"LGPL",
	               "committerLink":"https://svn.jboss.org/repos/gatein/components/pc/"
	            }
	         }
	      ]
	   }
	}
	
Important fields returned by the query
--------------------------------------

<table border="1">
<thead>
  <th>Field</th>
  <th>Example value</th>
  <th width="63%">Description</th>
</thead>
<tbody>
<tr><td align="center">hits</td><td></td><td>You will find the data of the result of the query inside</td></tr>
<tr><td align="center">total</td><td align="center">2</td><td>Number of records, that have been found.</td></tr>
<tr><td align="center">hits</td><td>array with records</td><td>You can find as many as <i>total</i> "_source" objects in this field.</td></tr>
<tr><td align="center">_source</td><td> </td><td>Each <i>_source</i> structure has an information about a single project as it is stored in DCP.</td></tr>
<tr><td align="center">sys_type</td><td align="center">"project_info"</td><td>Describes this dcp type. Always this value.</td></tr>
<tr><td align="center">sys_content_provider</td><td align="center">"jbossorg"</td><td>The provider of the data. Magnolia will supply jbossorg here. Other suppliers will have their own value.</td></tr>
<tr><td align="center">sys_content_type</td><td align="center">"jbossorg_project_info"</td><td>sys_content_provider and sys_type together.</td></tr>
<tr><td align="center">sys_id</td><td align="center">"jbossorg_project_info-gatein"</td><td>A unique identifier of the record in DCP. Use this to update already existing record in DCP.</td></tr>
<tr><td align="center">sys_content_id</td><td align="center">"gatein"</td><td>A value calculated from <i>nodePath</i> argument. It identifies the project in Magnolia. However to be more human readable and to prevent problems with rest ("/" is a problematic character) the first "/" is removed from nodePath and all other possible occurences of "/" are replaced by the underscore character ("_").</td></tr>
<tr><td align="center">sys_title</td><td align="center">"GateIn"</td><td>A human readable name of the project. It corresponds with <i>projectName</i> field. </td></tr>
<tr><td align="center">sys_url_view</td><td align="center">"/gatein"</td><td>An url to the project pages. If relative, i.e. "/gatein", the page can be displayed as "http://www.jboss.org/gatein". It may also be absolute.</td></tr>
<tr><td align="center">sys_description</td><td align="center">"Blah blah GateIn blah blah" </td><td>A human readable description of the project. It corresponds with <i>description</i> field.</td></tr>
<tr><td align="center">sys_updated</td><td align="center">"2013-01-02T12:18Z"</td><td>The date and the time when the information about the project was submited.</td></tr>
<tr><td colspan="2" align="center">... other....</td><td>Other fields correspond with the project property information fields. Please, see them <a href="https://www.jboss.org/help/awestructguide/projectpropertyfilestruction.html">here</a>.</td></tr>
</tbody>
</table>

*Note: Searchisko will add more fields to the result, however they are not yet specified and they should not contain any important information unless you try to debug the query.*