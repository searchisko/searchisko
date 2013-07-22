This folder contains configuration of rivers running inside Elasticsearch to pull
information from external sources into search indices,
see [http://www.elasticsearch.org/guide/reference/river/](http://www.elasticsearch.org/guide/reference/river/)

How to delete river:

- run: `curl -XDELETE http://localhost:15000/_river/jbossorg_jira/_meta`
- restart Elasticsearch node