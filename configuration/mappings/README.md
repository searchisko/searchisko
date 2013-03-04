This folder contains definitions of ElasticSearch mappings for distinct 
search types, see [http://www.elasticsearch.org/guide/reference/api/admin-indices-put-mapping.html](http://www.elasticsearch.org/guide/reference/api/admin-indices-put-mapping.html)

Subfolder is name of index where given mapping is used, name of file in 
subfolder is name of ElasticSearch type in given index with .json suffix added.

How to copy to OpenShift:

	zip -r mappings.zip **
	scp mappings.zip 5da3d60fa1034d1887eb4b8792c1bee2@dcp-jbossorgdev.rhcloud.com:~/app-root/data/
