/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.searchisko.api.testtools.ESRealClientTestBase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

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

	@Before
	public void setUp() throws Exception {
		try {
			logger.info("Starting in-memory H2 database for unit tests");
			Class.forName("org.h2.Driver");
			connection = DriverManager.getConnection("jdbc:h2:mem:unit-testing-jpa", "sa", "");
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail("Exception during H2 database startup.");
		}
		try {
			logger.info("Building JPA EntityManager for unit tests");
			emFactory = Persistence.createEntityManagerFactory("testPU");
			em = emFactory.createEntityManager();
		} catch (Exception ex) {
			ex.printStackTrace();
			Assert.fail("Exception during JPA EntityManager instanciation.");
		}
	}

	@After
	public void tearDown() throws Exception {
		logger.info("Shuting down Hibernate JPA layer.");
		if (em != null) {
			em.close();
		}
		if (emFactory != null) {
			emFactory.close();
		}
		logger.info("Stopping in-memory H2 database.");
		try {
			connection.createStatement().execute("SHUTDOWN");
		} catch (Exception ex) {
		}
	}

}
