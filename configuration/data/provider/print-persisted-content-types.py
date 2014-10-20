#!/usr/bin/python

# Get actual list of providers from Searchisko and print CURL command for reindex_from_persistence if content type is persisted
# Usage ./print-persisted-content-types.py -U searchisko base url -u username -p password


import sys, getopt
import json
import urllib2, base64

url = "http://localhost:8080"
username = "jbossorg"
password = "jbossorgjbossorg"

# Read command line args
try:
    myopts, args = getopt.getopt(sys.argv[1:], "U:u:p:")
except getopt.GetoptError as e:
    print (str(e))
    print("Usage: %s -U searchisko_base_url -u username -p password" % sys.argv[0])
    sys.exit(2)

for o, a in myopts:
    print o
    if o == '-U':
        url = a
    elif o == '-u':
        username = a
    elif o == '-p':
        password = a

print "Get All Content Types with persist enabled"
print "URL: ", url
print "Username: ", username

task_api = url + "/v2/rest/tasks/task/reindex_from_persistence"
providers_api = url + "/v2/rest/provider/"

request = urllib2.Request(providers_api)
base64string = base64.encodestring('%s:%s' % (username, password)).replace('\n', '')
request.add_header("Authorization", "Basic %s" % base64string)

try:
    json_data = urllib2.urlopen(request)
    data = json.load(json_data)
    json_data.close()
except urllib2.URLError as e:
    print "Cannot get data from Searchisko", e
    sys.exit(2)


for hit in data['hits']:
    print "PROVIDER: ", hit['data']['name']
    for content_type in hit['data']['type']:
        content_type_obj = hit['data']['type'][content_type];
        persist = content_type_obj.get('persist', False)
        if persist:
            print "curl -X POST -H \"Content-Type: application/json\" --user " + username + ":" + password + " " + task_api + " -d \'{\"sys_content_type\" : \"" + content_type + "\"}\'"