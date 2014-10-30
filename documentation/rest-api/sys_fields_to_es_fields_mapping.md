# Mapping from 'sys_*' fields to Elasticsearch \_index, \_type and \_id fields

When storing a documents into Searchisko we need to make sure that they can be uniquely distinguished and do not
unintentionally clash with other documents.

First we explain what schema is used to address documents in Elasticsearch. Then we explain how this translates to addressing in Searchisko.

### Elasticsearch index, type and id

In Elasticsearch it is pretty simple, documents live under `index`/`type` namespace.
When you [put](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/docs-index_.html) a document into
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
  "sys_url_view" : "https://developer.jboss.org/thread/158696"
}
```

When you index such document into Elasticsearch and [_search_](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/search.html) or [_get_](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.3/docs-get.html) it back later, it will look like this:

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
    "sys_url_view" : "https://developer.jboss.org/thread/158696"
  }
}
```

### Searchisko content provider, content type, ... etc

Now, in Searchisko documents live under differently organized (modelled) namespaces. And also it can do some more magic to your documents when you store them into Searchisko.

#### Content provider

An authenticated authority that is allowed to push new content (documents) into Searchisko is called a [content provider](management/content_provider.md) and its code goes into `sys_content_provider` field in every indexed document.
Each content provider can create documents of predefined types (list of predefined document types is part of content provider definition). This document type goes into `sys_content_type` field.

When you [post](http://docs.jbossorg.apiary.io/#post-%2Fv2%2Frest%2Fcontent%2F%7Bsys_content_type%7D%2F%7Bsys_content_id%7D)
a document into Searchisko you use the following notation:

```
curl --user ${username}:${password} -X POST <host>:<port>/v2/rest/content/{sys_content_type}/{sys_content_id} -d@json_file_name.json
```
where:

- `{username}`/`{password}` are the content provider credentials (so this gives us the  `sys_content_provider` value)
- `{sys_content_type}` is the document type defined by content provider
- `{sys_content_id}` is a unique document id within `{sys_content_type}`

Part of the document type definition within content provider configuration are names of Elasticsearch index and type which are used to index the new documents into. So once we know the content provider and document type we know which Elasticsearch `index`/`type` this document will be indexed into. We use provided `{sys_content_id}` to create `{sys_id}` which is used as a `{document_id}` value for underlying Elasticsearch index operation (see [Searchisko Content object](dcp_content_object.md) for more details).

When you index such document into Searchisko and [_search_](http://docs.jbossorg.apiary.io/#searchapi) for it later:

```
https://dcp-jbossorgdev.rhcloud.com/v2/rest/search?query=sys_id:jbossorg_sbs_forum-158696&field=_source
```
the response will look similar to the following:

```
{
  "_index" : "data_jbossorg_sbs_forum",
  "_type" : "jbossorg_sbs_forum",
  "_id" : "jbossorg_sbs_forum-158696",
  "_score" : 12.10193,
  "_source" : {
    "sys_id" : "jbossorg_sbs_forum-158696",
    "sys_activity_dates" : [ "2010-11-10T00:00:00.0-0000" ],
    "sys_content" : "Lorem ipsum dolor sit amet, consectetur adipisicing elit [...]",
    "sys_content_content-type" : "text/plain",
    "sys_content_id" : "158696",
    "sys_content_plaintext" : "Lorem ipsum dolor sit amet, consectetur adipisicing elit [...]",
    "sys_content_provider" : "jbossorg",
    "sys_content_type" : "jbossorg_sbs_forum",
    "sys_contributors" : [ "John Doe <john.doe@mymail.com>" ],
    "sys_created" : "2010-11-10T00:00:00.0-0000",
    "sys_description" : "Lorem ipsum dolor sit amet ...",
    "sys_last_activity_date" : "2010-11-10T00:00:00.0-0000",
    "sys_project" : "shrinkwrap",
    "sys_project_name" : "Shrinkwrap",
    "sys_title" : "John's Lorem",
    "sys_type" : "forumthread",
    "sys_updated" : "2010-11-10T00:00:00.0-0000",
    "sys_url_view" : "https://developer.jboss.org/thread/158696",
    "sys_rating_avg" : 4.3,
    "sys_rating_num" : 5
  }
}
```
Note that Searchisko did some modifications to the original document according to the [Searchisko configuration](https://github.com/searchisko/searchisko/tree/master/configuration) and [content provider configuration](https://github.com/searchisko/searchisko/tree/master/configuration/data/provider) for relevant `sys_content_provider` and particular `sys_content_type` input_preprocessors. For example it turned original `author` field into `sys_contributors` field containing full email address and name format (providing [contributors](https://github.com/searchisko/searchisko/tree/master/configuration/data/contributor) were configured accordingly) or it used original `date` value to populate several `sys_*` fields (e.g. `sys_activity_dates`, `sys_last_activity_date` or `sys_updated`) which are used at various occasions, like calculating `activity_dates_histogram` aggregation, sortBy by `old` or `new` … etc.
