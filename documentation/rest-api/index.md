DCP rest API index
==================

The DCP is able to search for the requested data. If you are authorized by the [JBoss.org team](http://www.jboss.org/about.html), you can also supply data to the system. One can use a REST API for both. If you are not familiar with the basic principles of DCP, please, read [this](http://docs.jbossorg.apiary.io/"Distributed Contribution Platform principles").

The DCP content can be characterized by a type (i.e. e-mails, blog posts, forum posts etc. See **dcp_type** in the basic principles article). Each *type* has its own logical structure and it will require specific fields to be filled.

You can find the content you can ask DCP for here and in case you were approved as the supplier of the data you will also find the necessary format for the submit. 

*Note: If you run your own instance of DCP, consider the documents in this folder as an inspiration. You will have your own content.*

Each folder in this directory has its name after the *dcp_type* field. Each of these directories will have at least one subdirectory that describes the current version. Their names will correspond with the version number (v1, v2.....).

 Usually the version with the highest number is the current one but the DCP update process counts with a temporary transition time when the two last versions will be valid. 

List of available *dcp_type*s
--------------------------

+ [project_info](project_info/v1/project_info.md) the data about the [JBoss.org projects](https://www.jboss.org/projects.html). 