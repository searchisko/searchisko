#!/bin/bash

## This script uses Elasticsearch 'search' cluster node http connector to init one concrete mapping for one named file.
##
## You have to use first commandline parameter to define relative path to mapping configuration json file in form of index/mapping.json 
## You can use second commandline parameter to define Elasticsearch http connector URL base. If not present then OPENSHIFT_JBOSSEAP_IP system property can be used to define IP/domainname part of URL (http protocol and port 15000 is used in this case). If not defined then default is: http://localhost:15000
## You can use optional third commandline parameter to define username for HTTP basic authentication
## You can use optional fourth commandline parameter to define password for HTTP basic authentication

clear

path=$1

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

code="${path%.*}"

echo "Go to init mapping ${code} into Elasticsearch: ${esurlbase}"

esurl=${esurlbase}/${code}/_mapping
echo
echo "Creating mapping ${path} on ES via ${esurl}"
curl ${auth} -XPUT -d@${path} ${esurl}
echo

echo "Finished"
