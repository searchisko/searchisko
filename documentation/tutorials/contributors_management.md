# Contributors management

## Abstract

This document explains how Searchisko works with Contributor informations and how to perform common management tasks around these data.
**DCP** is concrete instance of Searchisko configured for JBoss Community (jboss.org site).

## Content

### Types of Contributor related information in Searchisko

Searchisko uses two main types of contributor related informations. 

#### [Contributor](../rest-api/management/contributor.md) 
'Contributor' record contains distinct information/identifiers used in 'Content Push API' to find contributor `code` 
for `sys_contributors` field based on some identifier available in source data pushed from providers (eg. usernames from distinct system, email address, etc). 
This information is not considered as public and is accessible over specialized [Management API - contributors](http://docs.jbossorg.apiary.io/#managementapicontributors) only.

#### [Contributor Profile](../rest-api/content/contributor_profile.md) 
'Contributor Profile' contains public contributor profile informations like name, twitter id etc. 
It is common document (`sys_type`=`contributor_profile`) stored in Searchisko as any other data, and may be retrieved/searched using common 
[Search API](http://docs.jbossorg.apiary.io/#searchapi).

### Methods to create/update Contributor related informations in Searchisko

#### Manually

You can manage both Contributor and 'Contributor profile' data manually using APIs noted before.

When you add/change some normalization identifiers stored in 'Contributor' record, you should run 
some of [Content reindexation task](http://docs.jbossorg.apiary.io/#managementapicontentreindexationtasks) also 
to patch binding of existing data to the Contributor. Which task to use exactly depends on type of change you performed.

Also note that change of Contributor's `code` is not possible by common update API operation, as it can break data consistency.
Merging of two Contributors may lead to this problem also, so there are special operations in 'Management API - contributors' for these tasks.

#### Automatically from Profile Provider

Searchisko contains concept of "Profile provider", which may be used to manage data for 
both Contributor and 'Contributor profile' automatically from one source.
"Profile provider" is typically some remote user management system. 

Jive running at developer.jboss.org is used as profile provider for **DCP**, jboss.org username is stored in Contributor's record as `type_specific_code.jbossorg_username`.

Contributor and 'Contributor profile' is created/updated from 'Profile provider' in these cases:

* Contributor authenticates against Searchisko using [REST API's `/v2/rest/auth/status` operation](http://docs.jbossorg.apiary.io/#userauthenticationstatusapi) - jboss.org SSO session is used for **DCP** 
* Searchisko administrator runs `update_contributor_profile` [Task](http://docs.jbossorg.apiary.io/#managementapicontentreindexationtasks) 


### HowTo manage Contributors by Searchisko administrator

This chapter describes how to perform basic Contributors management operations

#### Create Contributor

You can always create Contributor record manually over 'Management API - contributors'.
Be careful not to use `email` or any of `type_specific_code` already used by another Contributor, as this is not checked by Searchisko. 
You can use `/contributor/search` operation to check this.

You can also use `update_contributor_profile` admin task to create Contributor and relevant 'Contributor profile' from 'Profile provider', eg.:

````
POST /v2/rest/tasks/task/update_contributor_profile
{
  "contributor_type_specific_code_type" : "jbossorg_username",
  "contributor_type_specific_code_value" : ["jdoyle","quimby","homers"]
}

````

When you know that newly created Contributor has some related content in Searchisko already, you should run 
some of [Content reindexation task](http://docs.jbossorg.apiary.io/#managementapicontentreindexationtasks) 
to bind the content to the Contributor. Which task to use exactly depends on type of change you performed, 
you can use:
 
* `renormalize_by_contributor_lookup_id` - you have to run it for all of `email` and `type_specific_code`
* `renormalize_by_content_type` - if you know the Contributors contribute some types of content only
* `renormalize_by_project_code` - if you know the Contributors contribute into some projects only

#### Update Contributor and Contributor Profile

You can update Contributor record manually over 'Management API - contributors'.
Be careful not to use `email` or any of `type_specific_code` already used by another Contributor, as this is not checked by Searchisko. 
You can use `/contributor/search` operation to check this.
You can't change Contributor's `code` by this method not to break data consistency, see special case later.  

You can also use `update_contributor_profile` admin task to update Contributor and relevant 'Contributor profile' from 'Profile provider', eg.:

````
POST /v2/rest/tasks/task/update_contributor_profile
{
  "contributor_type_specific_code_type" : "jbossorg_username",
  "contributor_type_specific_code_value" : ["jdoyle","quimby","homers"]
}

````

When you know that this update affected some of Contributor's mapping identifiers (`email`, `type_specific_code`), you should run 
some of [Content reindexation task](http://docs.jbossorg.apiary.io/#managementapicontentreindexationtasks) 
to bind existing content to the Contributor. Which task to use exactly depends on type of change you performed, 
you can use:
 
* `renormalize_by_contributor_lookup_id` - you have to run it for all of `email` and `type_specific_code` you added/removed
* `renormalize_by_content_type` - if you know the Contributors contribute some types of content only
* `renormalize_by_project_code` - if you know the Contributors contribute into some projects only


#### Bulk update of Contributor and Contributor Profile

You can use `update_contributor_profile` admin task to bulk 
update all Contributor with indicated `type_specific_code` and create/update relevant 'Contributor profile's from 'Profile provider'. eg.:

````
POST /v2/rest/tasks/task/update_contributor_profile
{
  "contributor_type_specific_code_type" : "jbossorg_username"
}

````

When you know that this update affected some of Contributor's mapping identifiers (`email`, `type_specific_code`), you should run 
some of [Content reindexation task](http://docs.jbossorg.apiary.io/#managementapicontentreindexationtasks) 
to bind existing content to the Contributor in same manner as for manual update.

#### Delete Contributor

You can update Contributor record manually over 'Management API - contributors'. Related 'Contributor profile' is deleted also.

You should run some of 'Content reindexation task' to remove relation to deleted Contributor from other content. 

Do not use raw Delete operation if you detect duplicity of some Contributor not to loose some data. Use special 'Merge' operation instead 
(see later)! 

#### Change Contributor's code

Contributor's `code` is used to bind other content to it (`sys_contributors` field in content), and have to be unique in whole Searchisko. 
It is typically constructed as `Full Name <primary email>`. 

To change this code for some contributor, you have to use special operation of 'Management API - contributors'.
This operation changes `code` and runs other operations to keep data integrity. Necessary 'Content reindexation task' is started automatically.

#### Merge two Contributors

If you detect case when there are two Contributor's in Searchisko which are one person in reality, you should use 
special 'merge' operation of 'Management API - contributors' to resolve this problem.
This operation merges Contributors into one and runs other operations to keep data integrity.
Necessary 'Content reindexation task' is started automatically.

#### Split Contributor to two

If you detect case when there is only one Contributor in Searchisko, but it contains content from two real persons, you should use this manual procedure:

* get record of existing Contributor from Searchisko   
* remove values from `email` and `type_specific_code` which are for second contributor and update first contributor back
* create new Contributor in Searchisko with his `email` and `type_specific_code` values
* run 'Content reindexation task' of type `renormalize_by_contributor_code` with code of old contributor (this task will rebind content from old to new Contributor based on changed configurations) 
