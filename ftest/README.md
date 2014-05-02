Searchisko Integration Test Suite
=================================

This module contains integration tests powered by Arquillian.

Set Up
------

It's necessary to have unpacked JBoss AS / JBoss EAP bundle.
Set environment variable `JBOSS_HOME` to AS location e.g.

	export JBOSS_HOME={your user home}/app/INTEG-TEST-jboss-eap-6.2.2

Integration tests are executed againts AS with `standalone-full.xml` profile as default.

Running tests
-------------

You can run individual test in you IDE or use maven to run all tests in project root (not in ftest directory)

Maven (integration test only):

	mvn test -Pskiptests,integration-tests


Maven (unit test in API module and integration tests)

	mvn test -Pintegration-tests


Log levels tuning
-----------------

It's good idea to show only relevant information in the log.
Navigate to `${JBOSS_HOME}/standalone/configuration/standalone.xml` and change console-handler level to FINEST:

    <subsystem xmlns="urn:jboss:domain:logging:1.3">
		<console-handler name="CONSOLE">
			<level name="FINEST"/>
			<formatter>
				<pattern-formatter pattern="%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n"/>
			</formatter>
		</console-handler>
		...

		but change root level to WARN and keep searchisko at INFO resp. FINEST for integration tests

		...

		<logger category="org.searchisko.ftest">
			<level name="FINEST"/>
		</logger>
		<logger category="org.searchisko">
			<level name="INFO"/>
		</logger>
		<root-logger>
			<level name="WARN"/>
			<handlers>
				<handler name="CONSOLE"/>
				<handler name="FILE"/>
			</handlers>
		</root-logger>
	</subsystem>


