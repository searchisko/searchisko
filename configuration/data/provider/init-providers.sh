#!/bin/bash

## This script uses system REST API to push content providers data
##
## You can use first commandline parameter to change base URL of system API call (/v2/rest/... is appended automatically to this base URL)
## You can use second commandline parameter to change system username
## You can use third commandline parameter to change system, password

clear

sysurl=https://dcp-jbossorgdev.rhcloud.com
if [ -n "${OPENSHIFT_JBOSSEAP_IP}" ]; then
  sysurl=http://${OPENSHIFT_JBOSSEAP_IP}:8080
fi
if [ -n "$1" ]; then
  sysurl=$1
fi

username=jbossorg
if [ -n "$2" ]; then
  username=$2
fi

password=jbossorgjbossorg
if [ -n "$3" ]; then
  password=$3
fi

outputfile=output.txt
sysapi=${sysurl}/v2/rest/provider/

echo "Pushing providers to sysytem API via ${sysapi}"
echo -n "" > $outputfile

for filename in *.json;
do
	echo -ne "Pushing $filename"

	output=$(curl -i -s -o $outputfile --user ${username}:${password} -w "%{http_code}" -H "Content-Type: application/json" -X POST -d@$filename ${sysapi})

	if [ "$output" == "200" ]; then
	  echo " [OK]"
	else
	  echo " [FAIL] - http code $output - check $outputfile"
	fi
done

echo "Finished"
