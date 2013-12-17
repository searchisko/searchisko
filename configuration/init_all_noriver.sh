#!/bin/bash

## This script uses DCP REST API
##
## You can use first commandline parameter to change base URL of DCP API call (/v1/rest/... is appended automatically to this base URL)
## You can use second commandline parameter to change DCP username
## You can use third commandline parameter to change DCP password

clear

dcpurl=https://dcp-jbossorgdev.rhcloud.com
if [ -n "$1" ]; then
  dcpurl=$1
fi

username=jbossorg
if [ -n "$2" ]; then
  username=$2
fi

password=jbossorgjbossorg
if [ -n "$3" ]; then
  password=$3
fi

eshost=localhost
if [ -n "${OPENSHIFT_JBOSSEAP_IP}" ]; then
  eshost=${OPENSHIFT_JBOSSEAP_IP}
fi
esport=15000

echo ========== Initializing ES cluster ===========
echo Using ES: ${eshost}:${esport}

pushd index_templates/
./init_templates.sh ${eshost} ${esport}
popd

pushd indexes/
./init_indexes.sh ${eshost} ${esport}
popd

pushd mappings/
./init_mappings.sh ${eshost} ${esport}
popd

echo ========== Initializing DCP ===========
echo Using dcp: ${dcpurl}

pushd data/provider/
./init-providers.sh ${dcpurl} ${username} ${password}
popd

pushd data/config/
./init-config.sh ${dcpurl} ${username} ${password}
popd

pushd data/project/
./init-projects.sh ${dcpurl} ${username} ${password}
popd

pushd data/contributor/
./init-contributors.sh ${dcpurl} ${username} ${password}
popd

echo FINISHED!
