#!/bin/bash

## This script uses ElasticSearch 'search' cluster node http connector
##
## You can use first commandline parameter or OPENSHIFT_JBOSSEAP_IP system property to change IP/domainname of ElasticSearch http connector. Default is: localhost
## You can use second commandline parameter to change port of ElasticSearch http connector. Default is: 15000

clear

esip="localhost";
if [ -n "${OPENSHIFT_JBOSSEAP_IP}" ]; then
  esip=${OPENSHIFT_JBOSSEAP_IP}
fi
if [ -n "$1" ]; then
  esip=$1
fi

esport=15000
if [ -n "$2" ]; then
  esport=$2
fi

echo "Elasticsearch: ${esip}:${esport}"


for filename in *.json;
do
    template="${filename%.*}"
    esurl=http://${esip}:${esport}/_template/${template}/
    echo
    echo "Creating index template based on ${filename} on ES via ${esurl}"
    curl -XPUT -d@${filename} ${esurl}
    echo
done

echo "Finished"

