# Content management

Content management is performed using [content manipulation API](http://docs.jbossorg.apiary.io/#contentmanipulationapi).
It is typically used when content is pushed into Searchisko by content providers from indexing jobs.


## Delete content manually

Sometimes it is needed to delete individual documents or whole content type manually. The following is an example
of how it can be done via CLI.

Imagine we want to delete all **Archetype** content. Archetype is stored in `jbossdeveloper_archetype` content type.

To delete the documents we need to get list of all document IDs (`sys_content_id`) first.
We can use the following `curl` command (tested on MacOS):

````
curl -s -X GET 'http://<dcp_server_URL>/v1/rest/search?type=jbossdeveloper_archetype&field=sys_content_id&size=5' \
  | python -mjson.tool \
  | grep sys_content_id \
  | cut -d':' -f2 \
  | sed 's/$/,/g' \
  | tr '\n' ' ' \
  | sed 's/,.$//g'
````

This will output something like:

````
"jboss-javaee6-webapp-archetype-old",  "jboss-html5-mobile-archetype-old",  "jboss-html5-mobile-archetype-wfk-24",  "jboss-javaee6-webapp-blank-archetype-old",  "jboss-javaee6-webapp-ear-archetype-old"
````

in this example we are pulling only 5 document IDs. In reality you will probably want to pull much more, like `size=200`.

Now we need to pass this output to the following DELETE command:

````
curl -X DELETE \
  'https://<dcp_server_URL>/v1/rest/content/jbossdeveloper_archetype' -d '{"id": [***ids_here***] }' \
  --user <provider_user>:<provider_pwd> \
  -H "Content-Type: application/json"
````

Note, the `***ids_here***` part needs to be replaced by the list of document IDs obtained above.
So it will look as follows:

````
curl -X DELETE \
  'https://<dcp_server_URL>/v1/rest/content/jbossdeveloper_archetype' -d '{"id": ["jboss-javaee6-webapp-archetype-old",  "jboss-html5-mobile-archetype-old",  "jboss-html5-mobile-archetype-wfk-24",  "jboss-javaee6-webapp-blank-archetype-old",  "jboss-javaee6-webapp-ear-archetype-old"] }' \
  --user <provider_user>:<provider_pwd> \
  -H "Content-Type: application/json"
````

Of course both the content provider name and password need to be provided. Pay attention to command output.

Repeat until you remove all needed documents (or use `size` large enough to do it in one shot).