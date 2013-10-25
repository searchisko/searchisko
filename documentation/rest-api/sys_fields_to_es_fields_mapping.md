# Mapping from 'sys_*' fields to Elasticsearch \_index, \_type and \_id fields

When storing a documents into Searchisko we need to make sure that they can be uniquely distinguished and do not
unintentionally clash with other documents.

First we explain what schema is used to address documents in Elasticsearch. Then we explain how this translates to addressing in Searchisko.

### Elasticsearch index, type and id

In Elasticsearch it is pretty simple, documents live under `index`/`type` namespace.
When you [put](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/docs-index_.html) a document into
Elasticsearch you use the following notation:

```
curl -X PUT <host>:<port>/{index_name}/{type_name}/{document_id} -d@some_json_file.json
```

where:

- `{index_name}`: Elasticsearch can manager multiple (Lucene) indices at the same time and when you index a new document it is always needed to specify the name of the index this document goes to. You can think of `{index_name}` as a *database name*.
- `{type_name}`: Type name is needed and is used to specify type of the document. You can think of it as a *database table*.
- `{document_id}`: This is a unique document id within `{index_name}`/`{type_name}` space. Two documents can not have the same id. (Elasticsearch does not require this value
to be provided in which case it will create a unique one automatically. However, as explained below Searchisko
requires the client [aka. `sys_content_provider`] to provide unique id when indexing a document.)

Let's have the following content in the `some_json_file.json` file:

```
{
  "author" : "John Doe",
  "date" : "10-21-2013",
  "subject" : "John's Lorem",
  "content" : "Lorem ipsum dolor sit amet, consectetur adipisicing elit [...]",
  "sys_url_view" : "https://community.jboss.org/thread/158696"
}
```

When you index such document into Elasticsearch and [_search_](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search.html) or [_get_](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/docs-get.html) it back later, it will look like this:

```
{
  "_index" : "{index_name}",
  "_type" : "{type_name}",
  "_id" : "{document_id}",
  "_score" : 1.0,
  "_source" : {
    "author" : "John Doe",
    "date" : "10-21-2013",
    "subject" : "John's Lorem",
    "content" : "Lorem ipsum dolor sit amet, consectetur adipisicing elit [...]",
    "sys_url_view" : "https://community.jboss.org/thread/158696"
  }
}
```

### Searchisko content provider, content type, ... etc

Now, in Searchisko documents live under differently organized (modelled) namespaces. And also it can do some more magic to your documents when you store them into Searchisko.

#### Content provider

An authenticated authority that is allowed to push new content (documents) into Searchisko is called a [content provider](management/content_provider.md) and its code goes into `sys_content_provider` field in every indexed document.
Each content provider can create documents of predefined types (list of predefined document types is part of content provider definition). This document type goes into `sys_content_type` field.

When you [post](http://docs.jbossorg.apiary.io/#post-%2Fv1%2Frest%2Fcontent%2F%7Bsys_content_type%7D%2F%7Bsys_content_id%7D)
a document into Searchisko you use the following notation:

```
curl --user ${username}:${password} -X POST <host>:<port>/v1/rest/content/{sys_content_type}/{sys_content_id} -d@json_file_name.json
```
where:

- `{username}`/`{password}` are the content provider credentials (so this identify `sys_content_provider` value)
- `{sys_content_type}` is the document type defined by content provider
- `{sys_content_id}` is a unique document id within `{sys_content_type}`

Part of the document type definition within content provider configuration are names of Elasticsearch index and type which are used to index the new documents into. So once we know the content provider and document type we know where to index the document. We use provided `{sys_content_id}` to create `{sys_id}` which is used as a `{document_id}` value for underlying Elasticsearch index operation.

See [DCP Content object](dcp_content_object.md) for more details.


```
{
  "_index" : "data_jbossorg_sbs_forum",
  "_type" : "jbossorg_sbs_forum",
  "_id" : "jbossorg_sbs_forum-158696",
  "_score" : 1.0,
  "_source" : {
    "authors" : [ {
      "display_name" : "yahya hugirat",
      "email_address" : "hugirat@gmail.com",
      "sys_contributor" : "yahya hugirat <hugirat@gmail.com>"
    } ],
    "source" : "jbossorg_sbs_forum",
    "space_key" : "2193",
    "sys_activity_dates" : [ "2010-11-10T03:32:05.804-0500" ],
    "sys_content" : "<body><p>hi all,</p><p>i need to add an app to my test archive it is a html2pdf converter, is there a way to that?</p><p>i want to test a webservice that convert html pages to pdf.</p><p/><p>Best regards</p><p>Yahya</p></body>",
    "sys_content_content-type" : "text/html",
    "sys_content_id" : "158696",
    "sys_content_plaintext" : "hi all, i need to add an app to my test archive it is a html2pdf converter, is there a way to that? i want to test a webservice that convert html pages to pdf. Best regards Yahya",
    "sys_content_provider" : "jbossorg",
    "sys_content_type" : "jbossorg_sbs_forum",
    "sys_contributors" : [ "yahya hugirat <hugirat@gmail.com>", "Aslak Knutsen <aslak@4fs.no>" ],
    "sys_created" : "2010-11-10T03:32:05.804-0500",
    "sys_description" : "hi all, i need to add an app to my test archive it is a html2pdf converter, is there a way to that? i want to test a webservice that convert html pages to pdf. Best regards Yahya",
    "sys_id" : "jbossorg_sbs_forum-158696",
    "sys_last_activity_date" : "2010-11-11T06:02:10.306-0500",
    "sys_project" : "shrinkwrap",
    "sys_project_name" : "Shrinkwrap",
    "sys_title" : "how to add a file to my archive?",
    "sys_type" : "forumthread",
    "sys_updated" : "2013-10-24T12:11:45.635-04:00",
    "sys_url_view" : "https://community.jboss.org/thread/158696"
  }
}
```
