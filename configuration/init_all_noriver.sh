#!/bin/bash

## This script uses both Searchisko REST API and Elasticsearch http API to init Elasticsearch indices and DCP configurations. No rivers are installed.
##
## You can use first commandline parameter to change base URL for Searchisko API call (/v2/rest/... is appended automatically to this base URL). If not present then OPENSHIFT_APP_DNS system property is be used. Default is http://localhost:8080
## You can use second commandline parameter to change Searchisko admin username
## You can use third commandline parameter to change Searchisko admin password
## You can use fourth commandline parameter to define Elasticsearch http connector URL base. If not present then OPENSHIFT_JBOSSEAP_IP system property can be used to define IP/domainname part of URL (http protocol and port 15000 is used in this case). If not defined then default is: http://localhost:15000
## You can use optional fifth commandline parameter to define username for Elasticsearch http connector HTTP basic authentication
## You can use optional sixth commandline parameter to define password for Elasticsearch http connector HTTP basic authentication


clear

searchiskourl=http://localhost:8080
if [ -n "${OPENSHIFT_APP_DNS}" ]; then
  searchiskourl=http://${OPENSHIFT_APP_DNS}
fi
if [ -n "$1" ]; then
  searchiskourl=$1
fi

searchiskousername=jbossorg
if [ -n "$2" ]; then
  searchiskousername=$2
fi

searchiskopassword=jbossorgjbossorg
if [ -n "$3" ]; then
  searchiskopassword=$3
fi

esurl="http://localhost:15000"
if [ -n "${OPENSHIFT_JBOSSEAP_IP}" ]; then
  esurl=http://${OPENSHIFT_JBOSSEAP_IP}:15000
fi

if [ -n "$4" ]; then
  esurl=$4
fi

if [ -n "$5" ]; then
  esusername=$5
fi

if [ -n "$6" ]; then
  espassword=$6
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

echo ========== Initializing Searchisko ===========
echo Using Searchisko REST API URL base: ${searchiskourl}

pushd data/provider/
./init-providers.sh ${searchiskourl} ${searchiskousername} ${searchiskopassword}
popd

pushd data/config/
./init-config.sh ${searchiskourl} ${searchiskousername} ${searchiskopassword}
popd

pushd data/project/
./init-projects.sh ${searchiskourl} ${searchiskousername} ${searchiskopassword}
popd

pushd data/contributor/
./init-contributors.sh ${searchiskourl} ${searchiskousername} ${searchiskopassword}
popd

echo FINISHED!
