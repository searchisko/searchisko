DCP Development Guide
=====================

## Technologies used

* ElasticSearch fulltext search engine
* JBoss EAP 6 - Java EE 6 - JAX-RS RestEasy, CDI, EJB Session beans, Hibernate JPA
* Jackson for JSON processing
* JUnit, Mockito for unit tests

## Build

It's necessary to use **Maven 3** to build this project! To build it simply use:

		mvn clean package

Build output is a `ROOT.war` file placed in the `deployments` folder.

The `pom.xml` file defines a few [build profiles](http://maven.apache.org/guides/introduction/introduction-to-profiles.html) 
used to build for different target environments (the `localhost` profile is activated by default):

#### localhost 

* build for development and testing in the localhost environment. Very easy deployment to the JBoss EAP 6 `standalone` configuration. 
* Embedded ElasticSearch nodes used with data stored in user home `.dcp_data` subfolder. Search node REST API on 15000 port. Statistics node REST API on 15100 port.  
* Embedded [h2 database](http://www.h2database.com) used for persistence over `java:jboss/datasources/ExampleDS` datasource which is configured in JBoss EAP 6 by default. 

#### openshift

* build for testing deployment on [OpenShift](http://openshift.redhat.com) 
* Embedded ElasticSearch nodes used with data stored in `OPENSHIFT_DATA_DIR` folder. Search node REST API on 15000 port. Statistics node REST API on 15100 port.
* MySQL OpenShift cartridge provided database for persistence.

#### production

* build for staging/production rollout
* remote ElasticSearch nodes. TODO details
* TODO database for persistence

## Deployment

#### localhost development

Build project with `localhost` development profile. 
Deploy `ROOT.war` to the JBoss EAP 6 `standalone` configuration, i.e. copy it 
to the `$EAP6HOME/standalone/deployments` folder. 
You can use [Eclipse with JBoss Tools](http://www.jboss.org/tools) or 
[JBoss Developer Studio](https://devstudio.jboss.com) for this.

The DCP REST API is then available at [`http://localhost:8080/v1/rest/`](http://localhost:8080/v1/rest/)  
The ElasticSearch search node REST API is available at [`http://localhost:15000/`](http://localhost:15000/)  

**Note**: When you use the standard configuration of the embedded h2 database from JBoss EAP 6 then data is lost 
when EAP is shutdown/restarted. To persist the data simply open the `standalone.xml` EAP configuration file, locate 
`<connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1</connection-url>` and change it to `<connection-url>jdbc:h2:test</connection-url>`.
Data is then persisted in the `$EAP6HOME/bin/test.h2.db` file so you can delete it manually if necessary.

**Note#2**: You might get the following exception when `ROOT.war` is deployed:


	JBAS018038: Root contexts can not be deployed when the virtual host configuration has the welcome root enabled, disable it and redeploy

In this case you need to change the `standalone.xml` configuration. Navigate to line:

	<virtual-server name="default-host" enable-welcome-root="true">

and change it to:

	<virtual-server name="default-host" enable-welcome-root="false">


#### OpenShift

Simply login to [OpenShift](https://openshift.redhat.com), create new Application 
with 'JBoss Enterprise Application Platform 6.0 Cartridge ' 
and add 'MySQL Database 5.1' cartridge into it. Then push content of this git 
repo into the OpenShift application's git repo. Then DCP is built and deployed automatically.

The DCP REST API is then available at `http://your_openshift_aplication_url/v1/rest/`  
The ElasticSearch search node REST API is available only from gear shell at `http://$OPENSHIFT_INTERNAL_IP:15000/`, 
you have to use port forwarding to access it from outside.

#### staging/production

TODO

## Initialization

After the DCP is deployed it's necessary to initialize it. Next initialization steps are necessary:

2. Create ElasticSearch index templates and indices over ElasticSearch REST API
3. Create ElasticSearch mappings over ElasticSearch REST API
4. Push all DCP init data (see [`rest-api/management`](rest-api/management) subfolder) over DCP management REST API in given order:
   - content providers
   - DCP configurations
   - projects
   - contributors
5. Initialize ElasticSearch rivers if any used to collect data 

Configuration used for jboss.org DCP instance is stored in
[`/configuration`](/configuration) folder of this repo. You can use it as
example for your DCP instance.

**Note** initial superprovider is automatically created with username `jbossorg`
and password `jbossorgjbossorg` during DCP first start. You can use it for 
initialization. It's highly recommended to change default 
password on publicly available instances!

