Searchisko Integration Test Suite
=================================

This module contains integration tests powered by Arquillian.

Set Up
------

It's necessary to have an unpacked JBoss AS / JBoss EAP bundle.
Set environment variable `JBOSS_HOME` to AS location e.g.

	export JBOSS_HOME={your user home}/app/INTEG-TEST-jboss-eap-6.2.2

Integration tests are executed against AS with `standalone.xml` profile as default.

It's needed to add security domain `SearchiskoSecurityDomainFTEST` into the `$JBOSS_HOME/standalone/configuration/standalone.xml` e.g.

		<subsystem xmlns="urn:jboss:domain:security:1.2">
            <security-domains>
				<security-domain name="SearchiskoSecurityDomainFTEST">
					<authentication>
						<login-module code="org.searchisko.api.security.jaas.ProviderLoginModule" flag="sufficient">
						</login-module>
						<login-module code="org.jboss.security.auth.spi.UsersRolesLoginModule" flag="sufficient">
							<module-option name="principalClass" value="org.searchisko.api.security.jaas.ContributorPrincipal" />
							<module-option name="usersProperties" value="searchisko-ftest-users.properties" /> 
							<module-option name="rolesProperties" value="searchisko-ftest-roles.properties" />
						</login-module>
					</authentication>
				</security-domain>
                ...
            </security-domains>
        </subsystem>

Note: Security domain name is intentionally different because functional tests uses HTTP Basic authentication to authenticate contributors.

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

