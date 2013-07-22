This folder contains definitions of Elasticsearch mappings for distinct
search types, see [http://www.elasticsearch.org/guide/reference/api/admin-indices-put-mapping.html](http://www.elasticsearch.org/guide/reference/api/admin-indices-put-mapping.html)

Subfolder is name of index where given mapping is used, name of file in 
subfolder is name of Elasticsearch type in given index with .json suffix added.

How to copy to OpenShift:

	zip -r mappings.zip **
	scp mappings.zip 5163d7b25973ca8ae4001fcf@dcp-jbossorgdev.rhcloud.com:~/app-root/data/
