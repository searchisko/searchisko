# Searchisko [![Build Status](https://travis-ci.org/searchisko/searchisko.svg?branch=master)](https://travis-ci.org/searchisko/searchisko) [![Coverage Status](https://coveralls.io/repos/searchisko/searchisko/badge.png?branch=master)](https://coveralls.io/r/searchisko/searchisko)

**Searchisko** is an open source project that allows to build a service
to index, search, retrieve and aggregate content from configurable resources.

It was started by [jboss.org Development Team](https://github.com/jbossorg) in order
to build a better search service for [community OSS projects](http://www.jboss.org/projects).

This system is necessary to support/extend synergy of JBoss Community in the era of more distributed development
environment, when project teams tend to use third party systems instead of systems provided and maintained
by JBoss Community Team.

Searchisko is Java EE 6 application intended to run in the JBoss EAP 6 application server and using Elasticsearch.

#### Mail list

- Developers: subscribe to [searchisko-dev](https://lists.jboss.org/mailman/listinfo/searchisko-dev) mail list and visit [archive](http://lists.jboss.org/pipermail/searchisko-dev/).

#### Release Notes

See [Release Notes](RELEASE_NOTES.md) for high level info about releases.

## DCP

Particular [configuration](configuration) and specific running instance of Searchisko tailored for JBoss.org needs
is called **DCP** (Distributed Contribution Platform). The DCP configuration can be also used as an example for anyone who
would like to use Searchisko in similar context but for different document resource.

### Searchisko Documentation

Anyone who would like to use Searchisko or implement a REST client for Searchisko can learn more in the following documentation:

- [Searchisko documentation](documentation/README.md).
- See [Searchisko basic principles document](documentation/basic_principles_and_architecture.md) for Searchisko overview and motivation behind it.
- Searchisko provides **REST API** for simple information manipulation and search/retrieval, see [`documentation`](documentation/README.md).