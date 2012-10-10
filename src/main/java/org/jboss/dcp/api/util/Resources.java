/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.util;

import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
 * This class uses CDI to alias Java EE resources, such as the persistence context, to CDI beans
 * 
 * <p>
 * Example injection on a managed bean field:
 * </p>
 * 
 * <pre>
 * &#064;Inject
 * private Logger log;
 * </pre>
 * 
 * @author Libor Krzyzanek
 */
@Singleton
public class Resources {

	@Produces
	public Logger produceLog(InjectionPoint injectionPoint) {
		return Logger.getLogger(injectionPoint.getMember().getDeclaringClass().getName());
	}

	@Produces
	@RequestScoped
	@Named("facesContext")
	public FacesContext produceFacesContext() {
		FacesContext ctx = FacesContext.getCurrentInstance();
		if (ctx == null) {
			throw new ContextNotActiveException("FacesContext is not active");
		}
		return ctx;
	}

}
