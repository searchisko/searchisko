#!/bin/bash

## This script uses system REST API to push one content provider
##
## You have to use first commandline parameter to define file with provider definition
## You can use second commandline parameter to change base URL of system API call (/v2/rest/... is appended automatically to this base URL)
## You can use third commandline parameter to change system username
## You can use fourth commandline parameter to change system, password

clear

filename=$1

sysurl=https://dcp-jbossorgdev.rhcloud.com
if [ -n "${OPENSHIFT_JBOSSEAP_IP}" ]; then
  sysurl=http://${OPENSHIFT_JBOSSEAP_IP}:8080
fi
if [ -n "$2" ]; then
  sysurl=$2
fi

username=jbossorg
if [ -n "$3" ]; then
  username=$3
fi

password=jbossorgjbossorg
if [ -n "$4" ]; then
  password=$4
fi

outputfile=output.txt
sysapi=${sysurl}/v2/rest/provider/

echo "Pushing provider $filename to sysytem API via ${sysapi}"
echo -n "" > $outputfile

output=$(curl -i -s -o $outputfile --user ${username}:${password} -w "%{http_code}" -H "Content-Type: application/json" -X POST -d@$filename ${sysapi})

if [ "$output" == "200" ]; then
  echo " [OK]"
else
  echo " [FAIL] - http code $output - check $outputfile"
fi

echo "Finished"
