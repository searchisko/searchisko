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

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeploymentHelpers {

	protected static Logger log = Logger.getLogger(DeploymentHelpers.class.getName());

	public static Properties appProperties;

	static {
		appProperties = new Properties();
		InputStream is = DeploymentHelpers.class.getResourceAsStream("/app.properties");
		try {
			appProperties.load(is);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Cannot load app.properties", e);
			throw new RuntimeException(e);
		}
	}

	protected static void removeSearchiskoDataDir() throws IOException {
		String d = appProperties.getProperty("es.client.embedded.data.path");
		log.log(Level.INFO, "Deleting searchisko data dir: {0}", d);
		FileUtils.deleteDirectory(new File(d));
	}

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		log.log(Level.INFO, "Creating Deployment");

		removeSearchiskoDataDir();

		// /api/pom.xml determined dynamically allowing running single test in IDE
		String rootPath = DeploymentHelpers.class.getResource("/").getFile();
		log.log(Level.FINEST, "Root Path for test classes: {0}", rootPath);

		final File[] runtimeDeps;
		try {
			// Root Path is /searchisko/ftest/target/test-classes/
			File projectRoot = new File(rootPath).getParentFile().getParentFile().getParentFile();
			log.log(Level.FINE, "Project Root: {0}", rootPath);

			File apiPom = new File(projectRoot.getAbsolutePath() + "/api/pom.xml");

			runtimeDeps = Maven.resolver().loadPomFromFile(apiPom).importRuntimeDependencies().resolve()
					.withTransitivity().asFile();

		} catch (Exception e) {
			log.log(Level.SEVERE, "Cannot create deployment for integration tests");
			throw e;
		}


		final WebArchive war = ShrinkWrap.create(WebArchive.class, "searchisko-contributorrestservice.war")
				.addPackages(true, "org.searchisko.api")
				.addPackages(true, "org.searchisko.contribprofile")
				.addPackages(true, "org.searchisko.persistence")
				.addAsLibraries(runtimeDeps)
				.addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
				.addAsWebInfResource("webapp/WEB-INF/web.xml", "web.xml")
				.addAsResource("systeminfo.properties")
				.addAsResource("app.properties")
				.addAsResource("search_timeouts.properties")
				.addAsResource("search_client_connections.properties")
				.addAsResource("search_client_settings.properties")
				.addAsResource("stats_client_connections.properties")
				.addAsResource("stats_client_settings.properties")
				.addAsResource("stats_client_configuration.properties")
				.addAsResource("mappings/contributor.json", "mappings/contributor.json")
				.addAsWebInfResource("webapp/WEB-INF/searchisko-ds.xml", "searchisko-ds.xml")
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

		return war;
	}
}
