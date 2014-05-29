#!/bin/bash

## This script uses Elasticsearch 'search' cluster node http connector to init mappings for one named index folder.
##
## You have to use first commandline parameter to define name of subfolder where mappings configuration json files are stored
## You can use second commandline parameter to define Elasticsearch http connector URL base. If not present then OPENSHIFT_JBOSSEAP_IP system property can be used to define IP/domainname part of URL (http protocol and port 15000 is used in this case). If not defined then default is: http://localhost:15000
## You can use optional third commandline parameter to define username for HTTP basic authentication
## You can use optional fourth commandline parameter to define password for HTTP basic authentication

clear

index=$1

esurlbase="http://localhost:15000";
if [ -n "${OPENSHIFT_JBOSSEAP_IP}" ]; then
  esurlbase=http://${OPENSHIFT_JBOSSEAP_IP}:15000
fi
if [ -n "$2" ]; then
  esurlbase=$2
fi

if [ -n "$3" ]; then
  auth="--user $3:$4"
fi

echo "Go to init mappings for index ${index} into Elasticsearch: ${esurlbase}"

if test -d $index; then
	pushd $index > /dev/null

	for filename in *.json;
	do
		code="${filename%.*}"
	    esurl=${esurlbase}/${index}/${code}/_mapping
        echo
	    echo "Creating mapping ${filename} on ES via ${esurl}"
        curl ${auth} -XPUT -d@${filename} ${esurl}
        echo
	done
	popd > /dev/null
fi

echo "Finished"
