#!/bin/bash

## This script uses Elasticsearch 'search' cluster node http connector
##
## You can use first commandline parameter or OPENSHIFT_JBOSSEAP_IP system property to change IP/domainname of Elasticsearch http connector. Default is: localhost
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

echo "Go to init mappings into Elasticsearch: ${esip}:${esport}"


for index in *;
do
  if test -d $index; then
	pushd $index > /dev/null

	for filename in *.json;
	do
		code="${filename%.*}"
	    esurl=http://${esip}:${esport}/${index}/${code}/_mapping
        echo
	    echo "Creating mapping ${filename} on ES via ${esurl}"
        curl -XPUT -d@${filename} ${esurl}
        echo
	done
	popd > /dev/null
  fi
done

echo "Finished"
