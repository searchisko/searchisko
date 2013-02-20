DCP REST API documentation
==========================

DCP REST API on-line documentation & Server Mock are available at [http://docs.jbossorg.apiary.io](http://docs.jbossorg.apiary.io/)

##DCP Content objects
DCP content is the main reason why DCP exists. It can be pushed into DCP by the 
registered providers over 'Content Push API' and searched over 'Search API' and 
'Query Suggestions API'.

The DCP content can be characterized by a type (i.e. e-mails, blog posts, forum 
posts, issues etc.). Content for one *dcp_type* can originate from distinct source 
systems (eg issue may be from JIRA or Bugzilla). 
Each *dcp_type* has its own logical structure and it will require specific fields 
to be filled. All types also share some common [system data fields](content/dcp_content_object.md).

You can find the description of the each content type available in DCP in 
[`content`](content) subfolder in this directory.

**Note:** If you run your own instance of DCP, consider the documents in this folder 
as an inspiration. You will have your own content.

###List of available *dcp_type*s

+ [project_info](content/project_info.md) the basic data about the [community project](https://www.jboss.org/projects.html). 
+ [blogpost](content/blogpost.md) the data about blog posts related to the project
+ [issue](content/issue.md) the data from project's issue tracker (JIRA, Bugzilla, 
  etc.) about project bugs, feature requests etc.

Other data types considered in the future:

+ maillist email
+ discussion forum topic
+ wiki page
+ documentation page
+ source code repository commit
+ IRC/IM conversation
+ maven repository artifact

##Data structures for Management API
Bunch of other informations is necessary to run DCP. They are managed over 
distinct parts of 'Management API'.
 
[`management`](management) subfolder in this directory contains files with 
description of document structures for management API:

+ [content provider](management/content_provider.md) - document type used by 'Management API - content providers'
+ [project](management/project.md) - document type used by 'Management API - projects'
+ [contributor](management/contributor.md) - document type used by 'Management API - contributors'
+ [config_*](management)  - document types used by 'Management API - configuration' to configure DCP

