This folder contains definitions of search indexes, 
see [http://www.elasticsearch.org/guide/reference/api/admin-indices-create-index.html](http://www.elasticsearch.org/guide/reference/api/admin-indices-create-index.html)

Note that prefixes are used to distinguish different index types:
 
* `data_`  - prefix for indices with searchable content (index template data_defaults.json is applied)
* `sys_`   - prefix for indices with system data (projects and contributors mapping definitions)
* `stats_` - prefix for indices with statistics/activity log data

How to copy to OpenShift:

	zip -r indexes.zip **
	scp indexes.zip 5da3d60fa1034d1887eb4b8792c1bee2@dcp-jbossorgdev.rhcloud.com:~/app-root/data/
