Searchisko configuration - configuration of CORS security for REST API
======================================================================================

**[configuration API](http://docs.jbossorg.apiary.io/#managementapiconfiguration) id:** `security_restapi_cors`

This configuration document defines restrictions for [Cross-Origin Resource Sharing (CORS)](http://www.w3.org/TR/cors/) security for Searchisko REST API.
If file is not present then there is not any CORS restriction on REST API, all origins can access it fully.

Definition MAY contain these fields:

* `origins` - contains array of origins which are allowed to access REST API. If this field is empty or not present 
  then all origins are allowed to access API. Origin is URI of the server the html which call REST API has been server 
  from, eg `http://myclient.org`, `http://localhost`, `null` (typically send by browser when html is served from `file:///`).  
* `origins_with_credentials` - contains array of origins which are allowed to access REST API as authenticated 
  (`Access-Control-Allow-Credentials` header is set to `true`). If this field is empty or not present then all 
  origins defined by `origins` field are allowed to do authenticated requests.

Example:

````
{
  "origins" : ["http://myclient.org", "https://adminclient.org"],
  "origins_with_credentials" : ["https://adminclient.org"]
}
````