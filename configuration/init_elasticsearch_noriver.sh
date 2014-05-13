#!/bin/bash

## This script uses Elasticsearch http API to init elasticsearch configurations only (indices and mappings)
##
## You can use first commandline parameter to define Elasticsearch http connector URL base. If not present then OPENSHIFT_JBOSSEAP_IP system property can be used to define IP/domainname part of URL (http protocol and port 15000 is used in this case). If not defined then default is: http://localhost:15000
## You can use optional second commandline parameter to define username for Elasticsearch http connector HTTP basic authentication
## You can use optional third commandline parameter to define password for Elasticsearch http connector HTTP basic authentication


clear

esurl="http://localhost:15000"
if [ -n "${OPENSHIFT_JBOSSEAP_IP}" ]; then
  esurl=http://${OPENSHIFT_JBOSSEAP_IP}:15000
fi

if [ -n "$1" ]; then
  esurl=$1
fi

if [ -n "$2" ]; then
  esusername=$2
fi

if [ -n "$3" ]; then
  espassword=$3
fi


echo ========== Initializing Elasticsearch cluster ===========
echo Using Elasticsearch http connector URL base: ${esurl}

pushd index_templates/
./init_templates.sh ${esurl} ${esusername} ${espassword}
popd

pushd indexes/
./init_indexes.sh ${esurl} ${esusername} ${espassword}
popd

pushd mappings/
./init_mappings.sh ${esurl} ${esusername} ${espassword}
popd

echo FINISHED!
