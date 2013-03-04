This folder contains configuration of rivers running inside DCP to pull
informations from external sources into DCP search indices, 
see [http://www.elasticsearch.org/guide/reference/river/](http://www.elasticsearch.org/guide/reference/river/)

How to delete river:

- run: `curl -XDELETE http://localhost:15000/_river/jbossorg_jira/_meta`
- restart ElasticSearch node