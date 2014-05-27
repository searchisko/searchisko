/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.ftest;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.event.container.AfterUnDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * Arquillian Extension for cleaning ES data dir before deployment and after undeployment events.
 *
 * @author Libor Krzyzanek
 * @see org.jboss.arquillian.container.spi.event.container.BeforeDeploy
 * @see org.jboss.arquillian.container.spi.event.container.AfterUnDeploy
 */
public class ESDataCleanupExtension implements LoadableExtension {

	protected static Logger log = Logger.getLogger(DeploymentHelpers.class.getName());

	@Override
	public void register(ExtensionBuilder builder) {
		builder.observer(ESCleanupObserver.class);
	}

	public static class ESCleanupObserver {

		public void beforeDeploy(@Observes final BeforeDeploy event) throws IOException {
			log.log(Level.INFO, "Deleting searchisko data dir before deployment.");
			DeploymentHelpers.removeSearchiskoDataDir();
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

}
