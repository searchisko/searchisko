/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * A class extending {@link Application} and annotated with @ApplicationPath is the Java EE 6 "no XML" approach to
 * activating JAX-RS.
 *
 * <p>
 * Resources are served relative to the servlet path specified in the {@link ApplicationPath} annotation.
 * </p>
 *
 * @author Libor Krzyzanek
 */
@ApplicationPath("/v2/rest")
public class JaxRsActivator extends Application {
	/* class body intentionally left blank */
}
