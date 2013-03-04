DCP jboss.org instance configuration
====================================

This folder contains definitions of distinct artifacts which must be 
initialized in DCP itself and backend ElasticSearch cluster for jboss.org 
DCP instance. You can use it as example for your instance.

DCP initialization steps:

1. Create ElasticSearch index templates - `index_templates/init_templates.sh`
2. Create ElasticSearch indexes  - `indexes/init_indexes.sh`
3. Create ElasticSearch mappings - `mappings/init_mappings.sh`
4. Push all DCP init data in given order:
   - `data/provider/init_providers.sh` 
   - `data/config/init_config.sh`
   - `data/project/init_projects.sh`
   - `data/contributor/init-contributors.sh`
5. Initialize ElasticSearch rivers - `rivers/init_rivers.sh` 

Note: each .sh script accepts commandline parameters which allows to configure 
location of DCP REST API or ElasticSearch search node http connector used by 
given script.


## Example - OpenShift

1. Zip all bits and copy to OpenShift

		zip -r init_data.zip  **
		scp init_data.zip  5da3d60fa1034d1887eb4b8792c1bee2@dcp-jbossorgdev.rhcloud.com:~/app-root/data/initial-configuration/


2. Connect to OpenShift and unzip

		ssh 5da3d60fa1034d1887eb4b8792c1bee2@dcp-jbossorgdev.rhcloud.com
		cd app-root/data/initial-configuration/
		unzip init_data.zip
		rm init_data.zip

3. Stop EAP and remove all DCP data

		ctl_app stop
		rm -rf ~/app-root/data/search
		rm -rf ~/app-root/data/stats
		
		mysql dcp;
		delete from Config; delete from Contributor; delete from Project; delete from Provider;
		exit;

4. Start EAP and push all init data

		ctl_app start
		./init_all_noriver.sh http://${OPENSHIFT_JBOSSEAP_IP}:8080
