Searchisko REST API documentation
=================================

Searchisko REST API on-line documentation & Server Mock are available at [http://docs.jbossorg.apiary.io](http://docs.jbossorg.apiary.io/)

##Searchisko Content object/Document
Searchisko content (Document) is the main reason why Searchisko exists. It can be pushed into the Searchisko by the 
registered providers over 'Content Push API' and searched over 'Search API' and 'Feed API'.

The Searchisko content can be characterized by a type (e.g. e-mails, blog posts, forum 
posts, issues, etc...). Content for one *sys_type* can originate from distinct source
systems (e.g. an issue may be from JIRA or Bugzilla). 
Each *sys_type* has its own logical structure and will require specific fields
to be filled. All types also share some common [system data fields](content/dcp_content_object.md).

You can find a description of each content type available in jboss.org Searchisko instance (sometimes called DCP) in the
[`content`](content) subfolder of this directory.

**Note:** If you run your own instance of the Searchisko, consider the documents in this folder 
as an inspiration. You will have your own content.

###List of available *sys_type*s

+ [project_info](content/project_info.md) the basic data about the [community project](https://www.jboss.org/projects.html). 
+ [contributor_profile](content/contributor_profile.md) the data about contributor profiles
+ [blogpost](content/blogpost.md) the data about blog posts related to the project
+ [issue](content/issue.md) the data from project's issue tracker (JIRA, Bugzilla, 
  etc...) about project bugs, feature requests etc.
+ [forumthread](content/forumthread.md) the data from project's discussion forum
+ [article](content/article.md) articles from project's wiki
+ [webpage](content/webpage.md) info about web page relate to the project
+ [video](content/video.md) metadata about video available for the project
+ [mailing_list_message](content/mailing_list_message.md) mbox message from project's mailing list
+ [addon](content/addon.md) description of some Addon or Plugin for the project
+ [solution](content/solution.md) description of some problem solution from knowledgebase
+ [documentation](content/documentation.md) info about documentation page related to the project 
+ [download](content/download.md) downloads data based on Apache access logs entries

Additional `sys_type`s can be found in
[jboss-developer](https://github.com/jboss-developer/www.jboss.org/blob/master/_dcp/data/provider/jboss-developer.json)
provider configuration.

Other data types considered in the future:

+ source code repository commit
+ IRC/IM conversation
+ maven repository artifact

##Data structures for Management API
A bunch of other information is necessary to run the Searchisko. This is managed using a 'Management API' and is not public.
 
The [`management`](management) subfolder in this directory contains files with 
descriptions of document structures for the management API:

+ [content provider](management/content_provider.md) - document type used by 'Management API - content providers'
+ [project](management/project.md) - document type used by 'Management API - projects'
+ [contributor](management/contributor.md) - document type used by 'Management API - contributors'
+ [query](management/query.md) - document type used by 'Management API - registered queries'
+ [config_*](management)  - document types used by 'Management API - configuration' to configure Searchisko instance

## Authentication and Roles based REST API security
User or client can be authenticated via two mechanisms where each is supposed for different use cases.

1. *Provider* authentication happens via HTTP Basic authentication. 
2. *Contributor* (called *user* sometimes) authentication via `/rest/auth/status` REST API call (which provides CAS SSO integration for now).

Once provider/user is authenticated then it can be granted by following roles which control access to distinct parts of REST API.

### Roles
1. `provider` - `default role` for authenticated *provider*. It is mainly used to control `/rest/content` API access.
2. `contributor` - `default role` for authenticated *contributor*
3. `admin` - system administrator with access to whole Management API
4. `contributors_manager` - full access to `/rest/contributor` Management API
5. `projects_manager` - full access to `/rest/project` Management API
6. `tasks_manager` - full access to `/rest/tasks` Management API
7. `tags_manager` - full acces to /rest/tagging API for all content types
8. `tags_manager_x` - access to /rest/tagging API for content type x

*Provider* can have only default `provider` role or can have `admin` role if defined in 
[provider configuration](management/content_provider.md).

*Contributors* can get any roles except `provider` by explicitly defining them in 
[contributor document](management/contributor.md).


## [Expert] Mapping to Elasticsearch indices

In the end of the day every indexed document is mapped and indexed into specific Elasticsearch index/type according on 
content provider configuration. Read more details about [Mapping from 'sys\_*' fields to Elasticsearch \_index, \_type and \_id fields](sys_fields_to_es_fields_mapping.md).

