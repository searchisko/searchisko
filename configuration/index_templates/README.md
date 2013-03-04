This folder contains definitions of search index templates. 
See [http://www.elasticsearch.org/guide/reference/api/admin-indices-templates.html](http://www.elasticsearch.org/guide/reference/api/admin-indices-templates.html)

* `data_defaults.json` is template applied to all indices with DCP content data 
(prefix `data_` used in the index name). It contains definition of custom analyzers 
and definitions of common dcp content document fields applied to all mappings. 

How to copy to OpenShift:

	zip -r index_templates.zip **
	scp index_templates.zip 5da3d60fa1034d1887eb4b8792c1bee2@dcp-jbossorgdev.rhcloud.com:~/app-root/data/
