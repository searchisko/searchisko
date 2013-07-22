DCP REST API documentation
==========================

DCP REST API on-line documentation & Server Mock are available at [http://docs.jbossorg.apiary.io](http://docs.jbossorg.apiary.io/)

##DCP Content objects
DCP content is the main reason why DCP exists. It can be pushed into the DCP by the 
registered providers over 'Content Push API' and searched over 'Search API' and 
'Query Suggestions API'.

The DCP content can be characterized by a type (e.g. e-mails, blog posts, forum 
posts, issues, etc...). Content for one *sys_type* can originate from distinct source
systems (e.g. an issue may be from JIRA or Bugzilla). 
Each *sys_type* has its own logical structure and will require specific fields
to be filled. All types also share some common [system data fields](content/dcp_content_object.md).

You can find a description of each content type available in the DCP in the
[`content`](content) subfolder of this directory.

**Note:** If you run your own instance of the DCP, consider the documents in this folder 
as an inspiration. You will have your own content.

###List of available *sys_type*s

+ [project_info](content/project_info.md) the basic data about the [community project](https://www.jboss.org/projects.html). 
+ [blogpost](content/blogpost.md) the data about blog posts related to the project
+ [issue](content/issue.md) the data from project's issue tracker (JIRA, Bugzilla, 
  etc...) about project bugs, feature requests etc.
+ [forumthread](content/forumthread.md) the data from project's discussion forum
+ [article](content/article.md) articles from project's wiki etc.

Other data types considered in the future:

+ mailing list email
+ documentation page
+ source code repository commit
+ IRC/IM conversation
+ maven repository artifact

##Data structures for Management API
A bunch of other information is necessary to run the DCP. This is managed using a 'Management API'.
 
The [`management`](management) subfolder in this directory contains files with 
descriptions of document structures for the management API:

+ [content provider](management/content_provider.md) - document type used by 'Management API - content providers'
+ [project](management/project.md) - document type used by 'Management API - projects'
+ [contributor](management/contributor.md) - document type used by 'Management API - contributors'
+ [config_*](management)  - document types used by 'Management API - configuration' to configure DCP

