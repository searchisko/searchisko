Searchisko Development Guide
============================

## Technologies used

* ElasticSearch fulltext search engine
* JBoss EAP 6.3 - Java EE 6 - JAX-RS RestEasy, CDI, EJB Session beans, Hibernate JPA
* Jackson for JSON processing
* JUnit, Mockito for unit tests
* Arquillian for functional tests

## Project structure

This is common **Maven** project with main `pom.xml` in root of repository. There are two subprojects available:

* `api` - main subproject with searchisko source codes
* `ftest` - subproject with Arquillian functional tests
 
Other folders in root of repository:

* `configuration` - example configurations for searchisko instance
* `documentation` - documentation for project
* `deployments` - output directory for build results

## Build

It's necessary to use **Maven 3** to build this project! To build it simply use:

		mvn clean package

in the root folder. Build output is a `ROOT.war` file placed in the `api/target` folder.

The `pom.xml` file defines a few [build profiles](http://maven.apache.org/guides/introduction/introduction-to-profiles.html) 
used to build for different target environments (the `localhost` profile is activated by default):

#### localhost 

* build for development and testing in the localhost environment. Very easy deployment to the JBoss EAP 6 `standalone` configuration. 
* Embedded ElasticSearch nodes used with data stored in user home `.dcp_data` subfolder. Search node REST API on 15000 port. Statistics node REST API on 15100 port.  
* Embedded [h2 database](http://www.h2database.com) used for persistence.

#### rhel6-dev

* build for development running on typical RHEL6 machine with installed standard JBoss EAP bundle.
* Embedded ElasticSearch nodes used with data stored in `_EAP_HOME_/standalone/.dcp_data` subfolder. Search node REST API on 15000 port. Statistics node REST API on 15100 port.
* Embedded [h2 database](http://www.h2database.com) used for persistence.

#### openshift

* build for testing deployment on [OpenShift](http://openshift.redhat.com) 
* Embedded ElasticSearch nodes used with data stored in `OPENSHIFT_DATA_DIR` folder. Search node REST API on 15000 port. Statistics node REST API on 15100 port.
* MySQL OpenShift cartridge provided database for persistence.

#### production

* build for staging/production rollout
* remote ElasticSearch nodes. TODO details
* TODO database for persistence

## Deployment

### Authentication and Authorization
Searchisko is secured by JAAS framework and application server needs to be configured.
Two modules are prepared for Provider authentication and CAS integration for Contributor authentication.
Just copy this security domain definition into `<subsystem xmlns="urn:jboss:domain:security:1.2"><security-domains>` section of `standalone.xml`:

	<security-domain name="SearchiskoSecurityDomain" cache-type="infinispan">
		<authentication>
			<login-module code="org.searchisko.api.security.jaas.ProviderLoginModule" flag="sufficient">
			</login-module>

			<login-module code="org.searchisko.api.security.jaas.ContributorCasLoginModule" flag="required">
				<module-option name="ticketValidatorClass" value="org.jasig.cas.client.validation.Cas20ServiceTicketValidator" />
				<module-option name="tolerance" value="20000" />
				<module-option name="defaultRoles" value="contributor" />
				<module-option name="roleAttributeNames" value="" />
				<module-option name="principalGroupName" value="CallerPrincipal"/>
				<module-option name="roleGroupName" value="Roles"/>
				<module-option name="cacheAssertions" value="true" />
				<module-option name="cacheTimeout" value="480" />
			</login-module>
		</authentication>
	</security-domain>

See how is Openshift configured in [Configuration](/.openshift/conf/standalone.xml) for instance.

### Cache
Searchisko needs number of caches to be configured:

1. cache for actual roles distribution which happens when contributor's roles are changed.
2. JAAS authentication cache

Standalone configuration:
Just copy this cache configuration into `<subsystem xmlns="urn:jboss:domain:infinispan:1.5">` section of `standalone.xml`:

	<cache-container name="security" default-cache="auth-cache">
		<local-cache name="auth-cache" batching="true">
			<expiration lifespan="10000"/>
		</local-cache>
	</cache-container>
	<cache-container name="searchisko">
		<local-cache name="searchisko-user-roles">
			<!-- Expiration - 30 mins - should be same as session expiration -->
			<expiration lifespan="1800000"/>
		</local-cache>
	</cache-container>


Clustered configuration:
TODO: Test cluster deployment 
Edit `domain/configuration/domain.xml` and add a this cache container configuration to the `full-ha` profile:

	<cache-container name="security" default-cache="auth-cache">
		<local-cache name="auth-cache" batching="true">
			<expiration lifespan="10000"/>
		</local-cache>
	</cache-container>
    <cache-container name="searchisko">
        <transport lock-timeout="60000"/>
        <replicated-cache name="searchisko-user-roles" mode="SYNC" batching="true">
        </replicated-cache>
    </cache-container>

[More info](https://docs.jboss.org/author/display/ISPN/Clustering+modes#Clusteringmodes-ReplicatedMode)

### Audit Log
Audit log is handled via Java Logging facility and thus can be tuned in AS logging configuration.
In JBoss AS it's wise to not mix audit logs with system logging and it can be achieved by logging configuration like this
(all within `<subsystem xmlns="urn:jboss:domain:logging:1.3">` subsystem):

Add logging size rotation handler handler:

	<size-rotating-file-handler name="AUDIT-FILE" autoflush="false">
		<formatter>
			<pattern-formatter pattern="%d{yyyy/MM/dd HH:mm:ss,SSS} %s%E%n" />
		</formatter>
		<file relative-to="jboss.server.log.dir" path="searchisko-audit.log" />
		<rotate-size value="20m" />
		<!-- <max-backup-index value="5" /> -->
		<append value="true" />
	</size-rotating-file-handler>

See [JBoss AS Documentation](https://docs.jboss.org/author/display/AS71/Logging+Configuration#LoggingConfiguration-sizerotatingfilehandler) for more details.

Point `org.searchisko.api.audit.handler.AuditHandlerLogging` implementation to this handler

	<logger category="org.searchisko.api.audit.handler.AuditHandlerLogging" use-parent-handlers="false">
		<level name="INFO" />
		<handlers>
			<handler name="AUDIT-FILE" />
		</handlers>
	</logger>

See how is Openshift configured in [Configuration](/.openshift/conf/standalone.xml) for instance.

#### localhost development

Build project with `localhost` development profile. 
Deploy `ROOT.war` to the JBoss EAP 6.3 `standalone` configuration, i.e. copy it
to the `$EAP6HOME/standalone/deployments` folder. 
You can use [Eclipse with JBoss Tools](http://www.jboss.org/tools) or 
[JBoss Developer Studio](https://devstudio.jboss.com) for this.

The Searchisko REST API is then available at [`http://localhost:8080/v2/rest/`](http://localhost:8080/v2/rest/)  
The ElasticSearch search node REST API is available at [`http://localhost:15000/`](http://localhost:15000/)  

**Note #1**: When you use the standard configuration of the embedded h2 database then data are lost
when EAP is shutdown/restarted. To persist the data simply open the `pom.xml` and change `datasource.connection.url` property for `localhost` profile from `jdbc:h2:mem:searchisko;DB_CLOSE_DELAY=-1` to `jdbc:h2:searchisko`.
Data is then persisted in the `$EAP6HOME/bin/searchislo.h2.db` file so you can delete it manually if necessary.

**Note #2**: You might get the following exception when `ROOT.war` is deployed:

	JBAS018038: Root contexts can not be deployed when the virtual host configuration has the welcome root enabled, disable it and redeploy

In this case you need to change the `standalone.xml` configuration. Navigate to line:

	<virtual-server name="default-host" enable-welcome-root="true">

and change it to:

	<virtual-server name="default-host" enable-welcome-root="false">

**Note #3**: You might need to bind EAP to externally available address to make it visible from other machines.
One way how to do this is via CL parameter when starting the server. For example if we need to bind the server to
IP `10.34.2.178`:

    ./bin/standalone.sh -b 10.34.2.178

#### rhel6-dev

No details yet. This is internal server.

#### OpenShift

Simply login to [OpenShift](https://openshift.redhat.com), create new Application 
with 'JBoss Enterprise Application Platform 6.0 Cartridge ' 
and add 'MySQL Database 5.5' cartridge into it.

Then push content of this git repo (`git push openshift master` or `git push openshift {local_branch_name}:master` if working on branch)
into the OpenShift application's git repo. Then Searchisko is built and deployed automatically.

The Searchisko REST API is then available at `http://your_openshift_aplication_url/v2/rest/`  
The ElasticSearch search node REST API is available only from gear shell at `http://$OPENSHIFT_JBOSSEAP_IP:15000/`,
you have to use port forwarding to access it from outside.

To enable Access to Contributor Profile Provider (developer.jboss.org) as authenticated set these [Openshift custom variables](https://www.openshift.com/blogs/taking-advantage-of-environment-variables-in-openshift-php-apps):

	rhc set-env SEARCHISKO_CB_PROVIDER_USERNAME=developer.jboss.org-username SEARCHISKO_CB_PROVIDER_PASSWORD=developer.jboss.org-password -a {name of your app}


#### staging/production

TODO

## Initialization

### Initialization from scratch

After the Searchisko is deployed it's necessary to initialize it. Next initialization steps are necessary:

2. Create ElasticSearch index templates and indices over ElasticSearch REST API
3. Create ElasticSearch mappings over ElasticSearch REST API
4. Push all Searchisko init data (see [`rest-api/management`](rest-api/management) subfolder) over Searchisko management REST API in given order:
   - content providers
   - Searchisko configurations
   - projects
   - contributors
5. Update Contributor profiles by executing `update_contributor_profile` task, e.g.:

		curl -X POST -H "Content-Type: application/json" --user jbossorg:jbossorgjbossorg https://your_openshift_aplication_url/v2/rest/tasks/task/update_contributor_profile -d '{"contributor_type_specific_code_type" : "jbossorg_username"}'

6. Initialize ElasticSearch rivers if any used to collect data

Configuration used for jboss.org Searchisko instance is stored in
[`/configuration`](/configuration) folder of this repo. You can use it as
example for your Searchisko instance.

**Note** initial superprovider is automatically created with username `jbossorg`
and password `jbossorgjbossorg` during Searchisko first start. You can use it for 
initialization. It's highly recommended to change default 
password on publicly available instances!

### Initialization from existing instance using DB backup

In case you have already running instance and you'd like to create same instance with same configuration/data follow these instructions for Openshift installation:

1. Create a new Openshfit instance. See section above.
2. Log in terminal to existing Openshift instance and create DB Backup
	
		mysqldump -h $OPENSHIFT_MYSQL_DB_HOST -P ${OPENSHIFT_MYSQL_DB_PORT:-3306} -u ${OPENSHIFT_MYSQL_DB_USERNAME} --password="$OPENSHIFT_MYSQL_DB_PASSWORD" --no-create-db "$OPENSHIFT_APP_NAME" | gzip > searchiskodb-backup.sql.gz

3. Copy dump to another instance (copy to your local machine and then to new instance to `~/app-root/data`)
4. Log in in terminal to the target instance and import dump

		gunzip < searchiskodb-backup.sql.gz | mysql -h $OPENSHIFT_MYSQL_DB_HOST -P ${OPENSHIFT_MYSQL_DB_PORT} -u ${OPENSHIFT_MYSQL_DB_USERNAME} --password="$OPENSHIFT_MYSQL_DB_PASSWORD" "$OPENSHIFT_APP_NAME"

5. Set Openshift environment properties if needed e.g. `SEARCHISKO_CB_PROVIDER_USERNAME` and `SEARCHISKO_CB_PROVIDER_PASSWORD` variables
6. Force push your current git repo to new Openshift instance
7. Initialize Searchisko like from scratch. See section above
8. Run task reindexation for each persisted content type. Example:

		curl -X POST -H "Content-Type: application/json" --user jbossorg:jbossorgjbossorg https://your_openshift_aplication_url/v2/rest/tasks/task/reindex_from_persistence -d '{"sys_content_type" : "jbossorg_blog"}'

To generate list of these commands from current configuration run

        configuration/data/provider/print-persisted-content-types.py -U https://your_openshift_aplication_url
        or use -u and -p arguments to pass different username and password
		configuration/data/provider/print-persisted-content-types.py -U https://your_openshift_aplication_url -u username -p password

### Initialization from existing instance using Openshift backup/restore

		rhc snapshot save searchisko1
		rhc app create searchisko2 jbosseap-6 mysql-5.5 -g medium
		rhc snapshot restore searchisko2 --filepath searchisko1.tar.gz
		rhc app-deploy master --app searchisko2

Note: app-deploy is needed because deployment replaces various variables by current Openshift values like `OPENSHIFT_MYSQL_DB_HOST` etc.
