# Content Security

## Abstract

This document explains all possibilities how to restrict access to distinct parts of content stored in Searchisko.

**Content Security is available from Searchisko 2.0!**

## Content

All Content Security options are intended to restrict access to distinct parts of content when retrieved over API's 
used to obtain content from Searchisko like "[Search API](http://docs.jbossorg.apiary.io/#searchapi)" and
"[Feed API](http://docs.jbossorg.apiary.io/#feedapi)".

Searchisko by default expects open content access, which means that if no any restriction is defined then 
all users (including not logged in users) can see given content.

All security restrictions are based on user roles, so only user with defined role can see some content or some parts of content.
Only few role names are predefined in Searchisko, but you can freely define your own role names during content restriction configuration, and then assign them to users.

**Tip:** All logged in Searchisko users have role `contributor` assigned, so you can use this one to restrict access for all logged in users.


### 'Content Type' level security

This kind of security allows you restrict access to whole content type originated from some source system. 
So for example you can restrict all issues originated from jboss.org JIRA to be visible only for users with some roles.   

Content types are defined in ["Content Provider" configuration file](../rest-api/management/content_provider.md). You can 
use `sys_visible_for_roles` field in `type` definition structure to define roles of users who can obtain documents of given type.

### Field level security

This kind of security allows you restrict access to some field stored in content. 
It is useful when your content is public but contains some parts which you want to hide for some users (eg. some sensitive information).
This security is system wide, so if you define restriction for some field then it apply to all documents from all content types.

Field level security is defined in ["Search response fields" configuration file](../rest-api/management/config_search_response_fields.md). 

### Document level security

This kind of security allows you to restrict access to exact 'Document' as you want. 
For example you can restrict access to some issues or blog posts.
It is defined using [`sys_visible_for_roles` field stored in Document itself](../rest-api/content/dcp_content_object.md).


### How to assign roles to the users

Roles are assigned to the user in his [Contributor configuration file](../rest-api/management/contributor.md).


