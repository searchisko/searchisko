#!/bin/bash

## This script uses ElasticSearch 'search' cluster node http connector.
##
## You can use first commandline parameter to define Elasticsearch http connector URL base. If not present then OPENSHIFT_JBOSSEAP_IP system property can be used to define IP/domainname part of URL (http protocol and port 15000 is used in this case). If not defined then default is: http://localhost:15000
## You can use optional second commandline parameter to define username for HTTP basic authentication
## You can use optional third commandline parameter to define password for HTTP basic authentication

clear

esurlbase="http://localhost:15000";
if [ -n "${OPENSHIFT_JBOSSEAP_IP}" ]; then
  esurlbase=http://${OPENSHIFT_JBOSSEAP_IP}:15000
fi
if [ -n "$1" ]; then
  esurlbase=$1
fi

if [ -n "$2" ]; then
  auth="--user $2:$3"
fi

echo "Go to init index templates into Elasticsearch: ${esurlbase}"

for filename in *.json;
do
    template="${filename%.*}"
    esurl=${esurlbase}/_template/${template}/
    echo
    echo "Creating index template based on ${filename} on ES via ${esurl}"
    curl ${auth} -XPUT -d@${filename} ${esurl}
    echo
done

echo "Finished"

