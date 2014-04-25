Searchisko Integration Test Suite
=================================

This module contains integration tests powered by Arquillian.

Set Up
------

It's necessary to have unpacked JBoss AS / JBoss EAP bundle.
Set environment variable `JBOSS_HOME` to AS location e.g.

	export JBOSS_HOME={your user home}/app/INTEG-TEST-jboss-eap-6.2.2

Running tests
-------------

You can run individual test in you IDE or use maven to run all tests in project root (not in ftest directory)

Maven (integration test only):

	mvn test -Pskiptests,integration-tests


Maven (unit test in API module and integration tests)

	mvn test -Pintegration-tests

