DCP REST API index
==================

DCP REST API on-line Documentation & Server Mock are available at <http://docs.jbossorg.apiary.io/>

##DCP Content objects
DCP content is the main reason why DCP exists. It can be pushed into DCP by the registered providers over 'Content Push API' and searched over 'Search API' and 'Query Suggestions API'.

The DCP content can be characterized by a type (i.e. e-mails, blog posts, forum posts, issues etc. See **dcp_type** in the basic principles article). 
Each *dcp_type* has its own logical structure and it will require specific fields to be filled. All types also share some common [system data fields](https://github.com/jbossorg/dcp-api/blob/master/documentation/rest-api/content/dcp_content_object.md).

You can find the description of the each content type available in DCP in [`content`](https://github.com/jbossorg/dcp-api/blob/master/documentation/rest-api/content/) subfolder in this directory.

*Note: If you run your own instance of DCP, consider the documents in this folder as an inspiration. You will have your own content.*

###List of available *dcp_type*s

+ [blogpost](https://github.com/jbossorg/dcp-api/blob/master/documentation/rest-api/content/blogpost.md) the data about blog posts related to the community projects.
+ [issue](https://github.com/jbossorg/dcp-api/blob/master/documentation/rest-api/content/issue.md) the data from community project's issue trackers (JIRA, Bugzilla, etc.) about project bugs, feature requests etc.
+ [project_info](https://github.com/jbossorg/dcp-api/blob/master/documentation/rest-api/content/project_info.md) the data about the [JBoss community projects](https://www.jboss.org/projects.html). 

##Data structures for Management API
Bunch of other informations is necessary to run DCP. They are managed over distinct parts of 'Management API'.
 
[`management`](https://github.com/jbossorg/dcp-api/blob/master/documentation/rest-api/management/) subfolder in this directory contains files with description of document structures for management API:

+ content provider - document type used by 'Management API - content providers'
+ project - document type used by 'Management API - projects'
+ contributor - document type used by 'Management API - contributors'
+ config_*  - document types used by 'Management API - configuration'

