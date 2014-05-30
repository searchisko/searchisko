/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.ftest;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.container.spi.event.container.AfterUnDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

/**
 * Arquillian Extension for cleaning ES data dir before deployment and after undeployment events.
 *
 * @author Libor Krzyzanek
 * @see org.jboss.arquillian.container.spi.event.container.BeforeDeploy
 * @see org.jboss.arquillian.container.spi.event.container.AfterUnDeploy
 */
public class SearchiskoWarmupExecutor {

	protected static Logger log = Logger.getLogger(SearchiskoWarmupExecutor.class.getName());

	@Inject
	private Instance<ProtocolMetaData> metaDataInst;

	// Currently no way to share @ArquillianResource URL (URLResourceProvider) logic internally, copied logic
	// See https://github.com/arquillian/arquillian-showcase/blob/master/extensions/resteasy/src/main/java/org/jboss/arquillian/extension/rest/client/RestInvoker.java
	private URI getBaseURL() {
		Collection<HTTPContext> contexts = metaDataInst.get().getContexts(HTTPContext.class);
		HTTPContext context = contexts.iterator().next();


		if (allInSameContext(context.getServlets())) {
			return context.getServlets().get(0).getBaseURI();
		}
		throw new IllegalStateException("No baseURL found in HTTPContext");
	}

	private boolean allInSameContext(List<Servlet> servlets) {
		Set<String> context = new HashSet<>();
		for (Servlet servlet : servlets) {
			context.add(servlet.getContextRoot());
		}
		return context.size() == 1;
	}

	public void beforeDeploy(@Observes final BeforeDeploy event) throws IOException {
		log.log(Level.INFO, "Deleting searchisko data dir before deployment.");
		DeploymentHelpers.removeSearchiskoDataDir();
	}

	public void afterDeploy(@Observes final AfterDeploy event) throws IOException {
		URL context = getBaseURL().toURL();

		log.log(Level.INFO, "Searchisko index templates initialization.");
		DeploymentHelpers.initIndexTemplates(context);
	}

	public void afterUndeploy(@Observes final AfterUnDeploy event) {
		log.log(Level.INFO, "Deleting searchisko data dir after deployment.");
		try {
			DeploymentHelpers.removeSearchiskoDataDir();
		} catch (IOException e) {
			// Don't throw anything since it's just a cleaning.
			log.log(Level.WARNING, "Cannot delete ES dir after undeploy: ", e);
		}
	}
}
