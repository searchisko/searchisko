# Searchisko

**Searchisko** is an open source project that allows to build a service
to index, search, retrieve and aggregate content from configurable resources.

It was started by [JBoss Community Team](https://github.com/jbossorg) in order
to build a better search service for [community projects](http://www.jboss.org/projects).

This system is necessary to support/extend synergy of JBoss Community in the era of more distributed development
environment, when project teams tend to use third party systems instead of systems provided and maintained
by JBoss Community Team.

Searchisko is Java EE 6 application intended to run in the JBoss EAP 6 application server and using Elasticsearch.

## DCP

Particular [configuration](configuration) and specific running instance of Searchisko tailored for JBoss.org needs
is called **DCP** (Distributed Contribution Platform). The DCP configuration can be also used as an example for anyone who
would like to use Searchisko in similar context but for different document resource.

### DCP Documentation

Anyone who would like to use DCP or implement a REST client for DCP can learn more in the following documentation:

- [DCP documentation](documentation/README.md).
- See [DCP basic principles document](documentation/basic_principles_and_architecture.md) for DCP overview and motivation behind it.
- DCP provides **REST API** for simple information manipulation and search/retrieval, see [`documentation`](documentation/README.md).
