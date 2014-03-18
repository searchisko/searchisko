Searchisko REST API documentation
=================================

Searchisko REST API on-line documentation & Server Mock are available at [http://docs.jbossorg.apiary.io](http://docs.jbossorg.apiary.io/)

##Searchisko Content objects
Searchisko content is the main reason why Searchisko exists. It can be pushed into the Searchisko by the 
registered providers over 'Content Push API' and searched over 'Search API' and 
'Query Suggestions API'.

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
+ [mailing_list_message](content/mailing_list_message.md) mbox message from project's mailing list

Other data types considered in the future:

+ documentation page
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
+ [config_*](management)  - document types used by 'Management API - configuration' to configure Searchisko instance

## [Expert] Mapping to Elasticsearch indices

In the end of the day every indexed document is mapped and indexed into specific Elasticsearch index/type according on content provider configuration. Read more details about [Mapping from 'sys\_*' fields to Elasticsearch \_index, \_type and \_id fields](sys_fields_to_es_fields_mapping.md).

