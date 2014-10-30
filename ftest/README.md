Searchisko Integration Test Suite
=================================

This module contains integration tests powered by Arquillian.

Note
----

If you get `LinkageError` during test run try to change version of JDK you use. This problem is with Oracle JDK 1.7.0_67 for Linux and downgrade to Oracle JDK 1.7.0_55 helped.


Set Up
------

It's necessary to have an unpacked JBoss AS / JBoss EAP bundle.
Set environment variable `JBOSS_HOME` to AS location e.g.

	export JBOSS_HOME={your user home}/app/INTEG-TEST-jboss-eap-6.3.0

Integration tests are executed against AS with `standalone.xml` profile as default.

It's needed to add security domain `SearchiskoSecurityDomainFTEST` into the `$JBOSS_HOME/standalone/configuration/standalone.xml` e.g.

		<subsystem xmlns="urn:jboss:domain:security:1.2">
            <security-domains>
				<security-domain name="SearchiskoSecurityDomainFTEST">
					<authentication>
						<login-module code="org.searchisko.api.security.jaas.ProviderLoginModule" flag="sufficient">
						</login-module>
						<login-module code="org.searchisko.api.security.jaas.UsersRolesForPrincipalWithRolesLoginModule" flag="sufficient">
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
It also doesn't have any authentication cache

Copy this cache configuration into `<subsystem xmlns="urn:jboss:domain:infinispan:1.5">` section of `standalone.xml`:

	<cache-container name="searchisko">
		<local-cache name="searchisko-user-roles">
			<!-- Expiration - 30 mins - should be same as session expiration -->
			<expiration lifespan="1800000"/>
		</local-cache>
	</cache-container>

See [JBoss EAP 6.3 standalone.xml example](src/conf/jboss-eap-6.3-standalone.xml) how it can looks like


Running tests
-------------

You can run individual test in you IDE or use maven to run all tests in project root (not in ftest directory)

Maven - integration test only:

	mvn test -Pskiptests,integration-tests
	
Maven - specific integration test class:
	
    mvn test -Pskiptests,integration-tests -Dtest=ProjectRestServiceTest

Maven - unit test in API module and integration tests:

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

