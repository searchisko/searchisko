#!/bin/bash

## This script uses Searchisko REST API to init DCP configurations only (no ES indices, no rivers)
##
## You can use first commandline parameter to change base URL for Searchisko API call (/v2/rest/... is appended automatically to this base URL). If not present then OPENSHIFT_APP_DNS system property is be used. Default is http://localhost:8080
## You can use second commandline parameter to change Searchisko admin username
## You can use third commandline parameter to change Searchisko admin password

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
