/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.sql.Connection;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.Session;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.searchisko.api.testtools.ESRealClientTestBase;

/**
 * Unit test for {@link JpaEntityService}
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class JpaTestBase extends ESRealClientTestBase {

	protected Logger logger = Logger.getLogger(JpaTestBase.class.getName());

	protected EntityManagerFactory emFactory;

	protected EntityManager em;

	protected Connection connection;

	public DriverManagerConnectionProviderImpl getConnectionProvider() {
		SessionFactoryImpl factory = (SessionFactoryImpl) em.unwrap(Session.class).getSessionFactory();
		return (DriverManagerConnectionProviderImpl) factory.getConnectionProvider();
	}

	@Before
	public void setUp() throws Exception {
		try {
			logger.info("Building JPA EntityManager for unit tests");
			emFactory = Persistence.createEntityManagerFactory("testPU");
			em = emFactory.createEntityManager();
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail("Exception during JPA EntityManager initialization.");
		}
	}

	@After
	public void tearDown() throws Exception {
		logger.info("Shutting down Hibernate JPA layer.");
		if (em != null) {
			em.close();
		}
		if (emFactory != null) {
			emFactory.close();
		}
	}

}
