Contributor Profile Search API
==============================

**sys\_type = "contributor\_profile"**

This data type stored in Searchisko contains additional information about contributor
Contains data from [JBoss Community](https://community.jboss.org).


An example of the search results:
---------------------------------

	{
	   "hits":{
	      "total":2,
	      "hits":[
	         {
	            "_source":{
	            }
	         },
	         {
	            "_source":{
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
<tr><td align="center">_source</td><td> </td><td>Each <i>_source</i> structure has an information about a single contributor profile stored in Searchisko.</td></tr>
</tbody>
</table>
