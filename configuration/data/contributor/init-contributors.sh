#!/bin/bash

## This script uses DCP REST API
##
## You can use first commandline parameter to change base URL of DCP API call (/v1/rest/... is appended automatically to this base URL)
## You can use second commandline parameter to change DCP username
## You can use third commandline parameter to change DCP password

clear

dcpurl=https://dcp-jbossorgdev.rhcloud.com
if [ -n "${OPENSHIFT_JBOSSEAP_IP}" ]; then
  dcpurl=http://${OPENSHIFT_JBOSSEAP_IP}:8080
fi
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

outputfile=output.txt
dcpcontributorapi=${dcpurl}/v1/rest/contributor/

echo "Pushing Contributors to DCP API via ${dcpcontributorapi}"
echo -n "" > $outputfile

for filename in *.json;
do
	code="${filename%.*}"
	echo -ne "Pushing $code"

	output=$(curl -i -s -o $outputfile --user ${username}:${password} -w "%{http_code}" -H "Content-Type: application/json" -X POST -d@$filename ${dcpcontributorapi}$code)

	if [ "$output" == "200" ]; then
	  echo " [OK]"
	else
	  echo " [FAIL] - http code $output - check $outputfile"
      break
	fi
done

echo "Finished"
