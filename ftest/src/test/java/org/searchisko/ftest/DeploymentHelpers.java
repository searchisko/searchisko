/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.searchisko.ftest;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jayway.restassured.http.ContentType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.FilteredStringAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.searchisko.ftest.rest.SystemRestServiceTest;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

public class DeploymentHelpers {

	protected static Logger log = Logger.getLogger(DeploymentHelpers.class.getName());

	public static final String DEFAULT_API_VERSION = "v2/";

	public static final String DEFAULT_REST_VERSION = DEFAULT_API_VERSION + "rest/";

	public static final String DEFAULT_PROVIDER_NAME = "jbossorg";

	public static final String DEFAULT_PROVIDER_PASSWORD = "jbossorgjbossorg";

	public static final ProviderModel DEFAULT_PROVIDER = new ProviderModel(DEFAULT_PROVIDER_NAME, DEFAULT_PROVIDER_PASSWORD);

	public static final String SECURITY_DOMAIN = "SearchiskoSecurityDomainFTEST";

	public static Properties appProperties;

	static {
		appProperties = new Properties();
		InputStream is = DeploymentHelpers.class.getResourceAsStream("/ftest-settings.properties");
		try {
			appProperties.load(is);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Cannot load ftest-settings.properties", e);
			throw new RuntimeException(e);
		}
	}

	protected static void removeSearchiskoDataDir() throws IOException {
		String d = appProperties.getProperty("es.client.embedded.data.path");
		log.log(Level.INFO, "Deleting searchisko data dir: {0}", d);
		FileUtils.deleteDirectory(new File(d));
	}

	public static final HashMap<String, String> webXMLReplacements = new HashMap<>();

	static {
		webXMLReplacements.put("${cas.serverName}", "http://localhost:8080");
		webXMLReplacements.put("${cas.ssoServerUrl}", "https://localhost:8443");
	}

	public static Asset getWebXML(String projectRootPath) throws IOException {
		String originalWebXML = FileUtils.readFileToString(new File(projectRootPath + "/api/src/main/webapp/WEB-INF/web.xml"));
		return new FilteredStringAsset(originalWebXML, webXMLReplacements);
	}


	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		log.log(Level.INFO, "Creating default deployment");
		return createWar();
	}

	@Deployment(testable = false)
	public static WebArchive createDeploymentMinimalWebXML() throws IOException {
		log.log(Level.INFO, "Creating default deployment");

		WebArchive war = createWar();
		war.setWebXML("webapp/WEB-INF/web-minimal.xml");

		return war;
	}

	private static String projectRootPath = null;


	public static String getProjectRootPaht() {
		if (projectRootPath == null) {
			String rootPath = DeploymentHelpers.class.getResource("/").getFile();
			// Root Path is /searchisko/ftest/target/test-classes/
			File projectRoot = new File(rootPath).getParentFile().getParentFile().getParentFile();
			log.log(Level.FINE, "Project Root: {0}", rootPath);
			projectRootPath = projectRoot.getAbsolutePath();
			log.log(Level.FINEST, "Root Path for test classes: {0}", projectRootPath);
		}
		return projectRootPath;
	}

	public static File getProjectFile(String relativePath) {
		return new File(getProjectRootPaht() + relativePath);
	}

	protected static WebArchive createWar() throws IOException {
		final File[] runtimeDeps;
		try {
			// /api/pom.xml determined dynamically allowing running single test in IDE
			File apiPom = new File(getProjectRootPaht() + "/api/pom.xml");

			runtimeDeps = Maven.resolver().loadPomFromFile(apiPom).importRuntimeDependencies().resolve()
					.withTransitivity().asFile();

		} catch (Exception e) {
			log.log(Level.SEVERE, "Cannot create deployment for integration tests");
			throw e;
		}

		WebArchive war = ShrinkWrap.create(WebArchive.class, "searchisko-ftest.war")
				.addAsLibraries(runtimeDeps)
				.addPackages(true, "org.searchisko.api")
				.addPackages(true, "org.searchisko.contribprofile")
				.addPackages(true, "org.searchisko.persistence")
				.addAsResource("searchisko-ftest-users.properties")
				.addAsResource("searchisko-ftest-roles.properties")
				.addAsResource("systeminfo.properties")
				.addAsResource("app.properties")
				.addAsResource("search_timeouts.properties")
				.addAsResource("search_client_settings.properties")
				.addAsResource("stats_client_configuration.properties")
				.addAsResource("mappings/contributor.json", "mappings/contributor.json")
				.addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
				.setWebXML(getWebXML(projectRootPath))
				.addAsWebInfResource(new StringAsset("<jboss-web><security-domain>" + SECURITY_DOMAIN
						+ "</security-domain></jboss-web>"), "jboss-web.xml")
				.addAsWebInfResource("webapp/WEB-INF/test-searchisko-ds.xml", "searchisko-ds.xml")
				.addAsWebInfResource(new File(projectRootPath + "/api/src/main/webapp/WEB-INF/beans.xml"), "beans.xml")
				.addAsWebInfResource(new File(projectRootPath + "/api/src/main/webapp/WEB-INF/jboss-deployment-structure.xml"), "jboss-deployment-structure.xml");

		return war;
	}

	/**
	 * Refresh Elastic Search to get data up to date
	 *
	 * @throws MalformedURLException
	 */
	public static void refreshES() throws MalformedURLException {
		String esPort = appProperties.getProperty("es.client.embedded.search.port.start");
		int port = Integer.parseInt(esPort);

		String esIp = appProperties.getProperty("es.client.embedded.search.ip");

		String url = new URL("http", esIp, port, "/_refresh").toExternalForm();
		log.log(Level.INFO, "Refreshing Elastic Search, url: {0}", url);

		given().contentType(ContentType.JSON)
				.body("")
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.when().post(url);
	}

	public static void initIndexTemplates(URL context) throws IOException {
		initIndexTemplate(context, "defaults");
		initIndexTemplate(context, "data_defaults");
		initIndexTemplate(context, "stats_defaults");
	}

	public static void initIndexTemplate(URL context, String templateName) throws IOException {
		final String relativePath = "/configuration/index_templates/" + templateName + ".json";

		log.log(Level.INFO, "Init index template {0} via context {1}", new Object[]{relativePath, context});
		File file = getProjectFile(relativePath);
		InputStream is = new FileInputStream(file);
		byte[] data = IOUtils.toByteArray(is);
		is.close();

		given().contentType(ContentType.JSON)
				.pathParam("operation", SystemRestServiceTest.OPERATION_ES)
				.auth().basic(DEFAULT_PROVIDER_NAME, DEFAULT_PROVIDER_PASSWORD)
				.body(data)
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("acknowledged", is(true))
				.when().put(new URL(context, SystemRestServiceTest.SYSTEM_REST_API + "/search/_template/" + templateName).toExternalForm());
	}
}
