/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.infinispan.manager.EmbeddedCacheManager;

/**
 * This class uses CDI to alias Java EE resources, such as the persistence context, to CDI beans
 * <p/>
 * <p>
 * Examples injection on a managed bean field:
 * </p>
 * <p/>
 * 
 * <pre>
 * &#064;Inject
 * private Logger log;
 * </pre>
 * <p/>
 * 
 * <pre>
 * &#064;Inject
 * private EntityManager em;
 * </pre>
 * 
 * @author Libor Krzyzanek
 */
@Singleton
@Lock(LockType.READ)
public class Resources {

	@Produces
	@PersistenceContext
	private static EntityManager em;

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

	@Produces
	@Resource(lookup = "java:jboss/infinispan/container/searchisko")
	private static EmbeddedCacheManager container;

	/**
	 * Read file from classpath into String. UTF-8 encoding expected.
	 * 
	 * @param filePath in classpath to read data from.
	 * @return file content.
	 * @throws IOException
	 */
	public static String readStringFromClasspathFile(String filePath) throws IOException {
		StringWriter stringWriter = new StringWriter();
		InputStreamReader input = new InputStreamReader(Resources.class.getResourceAsStream(filePath), "UTF-8");
		try {
			char[] buffer = new char[1024 * 4];
			int n = 0;
			while (-1 != (n = input.read(buffer))) {
				stringWriter.write(buffer, 0, n);
			}
			return stringWriter.toString();
		} finally {
			input.close();
		}
	}

}
