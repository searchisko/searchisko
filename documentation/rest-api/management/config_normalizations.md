Searchisko configuration - configuration of normalizations for "Normalization REST API"
======================================================================================

**configuration API id:** `normalizations`

This configuration document contains definition of normalizations which can be performed using 
Searchisko 'Normalization API' `normalization` operation. It contains map structure where key is 
name of normalization (used to call it over REST API) and value is structure containing normalization definition.
For now only supported field here is `preprocessors`, which allows to define chain of [preprocessors](https://github.com/searchisko/structured-content-tools) 
used to perform given normalization when called over REST API. 

Data passed to this preprocessor chain contain `input_id` field, which contains identifier passed over REST API to perform normalization for.
Everything preprocessors add to the data is returned in REST API response for given normalization. 
Output may contain `warnings` field also, which contains warnings produced by preprocessors.    

Example:

````
{
  "contributor_id_by_username" : {
    "preprocessors" : [
      { 
        "name"     : "username to Contributor code mapper",
        "class"    : "org.jboss.elasticsearch.tools.content.ESLookupValuePreprocessor",
        "settings" : {
          "index_name"       : "sys_contributors",
          "index_type"       : "contributor",
          "source_field"     : "input_id",
          "idx_search_field" : "type_specific_code.username",
          "result_mapping"   : [{
            "idx_result_field" : "code",
            "target_field"     : "sys_contributor"
          }]
        } 
      }
    ]
  },
  "contributor_profile_by_email" : {
    "preprocessors" : [
      { 
        "name"     : "email to Contributor code mapper",
        "class"    : "org.jboss.elasticsearch.tools.content.ESLookupValuePreprocessor",
        "settings" : {
          "index_name"       : "sys_contributors",
          "index_type"       : "contributor",
          "source_field"     : "input_id",
          "idx_search_field" : "email",
          "result_mapping"   : [{
            "idx_result_field" : "code",
            "target_field"     : "sys_contributor"
          }]
        } 
      },{ 
        "name"     : "Profile by Contributor code loader",
        "class"    : "org.jboss.elasticsearch.tools.content.ESLookupValuePreprocessor",
        "settings" : {
          "index_name"       : "data_contributor_profile",
          "index_type"       : "jbossorg_contributor_profile",
          "source_field"     : "sys_contributor",
          "idx_search_field" : "sys_contributors",
          "result_mapping"   : [{
              "idx_result_field" : "sys_title",
              "target_field"     : "contributor_profile.sys_title"
            },{
              "idx_result_field" : "sys_url_view",
              "target_field"     : "contributor_profile.sys_url_view"
            },{
              "idx_result_field" : "thumbnailUrl",
              "target_field"     : "contributor_profile.thumbnailUrl"
          }]
        } 
      }
    ]
  }
  ````