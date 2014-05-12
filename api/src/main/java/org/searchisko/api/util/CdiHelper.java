package org.searchisko.api.util;

import org.hibernate.Session;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.jdbc.connections.internal.DatasourceConnectionProviderImpl;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.sql.DataSource;

/**
 * CDI Helper utility
 *
 * @author Libor Krzyzanek
 */
public class CdiHelper {


	/**
	 * Get Default DataSource. JNDI name of datasource is taken from Entity Manager
	 *
	 * @return
	 * @throws NamingException
	 */
	public static DataSource getDefaultDataSource(EntityManager em) throws NamingException {
// Doesn't work because of https://hibernate.atlassian.net/browse/HHH-8121
//		EntityManagerFactory emFactory = em.getEntityManagerFactory();
//		Object ds = emFactory.getProperties().get("javax.persistence.jtaDataSource");
//		InitialContext initialContext = new InitialContext();
//		Object lookup = initialContext.lookup(ds.toString());
//		return (DataSource) lookup;

// TODO: Find better way how to get DataSource from JPA without using hard coded JNDI name or casting to JPA implementation
		SessionFactoryImpl factory = (SessionFactoryImpl) em.unwrap(Session.class).getSessionFactory(); // or directly cast the sessionFactory
		DatasourceConnectionProviderImpl provider = (DatasourceConnectionProviderImpl)factory.getConnectionProvider();
		return provider.getDataSource();
	}

	/**
	 * Programatically inject dependencies to desired bean.
	 * It's shortcut for programmaticInjection(injectionObject.getClass(), injectionObject);
	 *
	 * @param injectionObject
	 * @throws NamingException
	 */
	public static void programmaticInjection(Object injectionObject) throws NamingException {
		programmaticInjection(injectionObject.getClass(), injectionObject);
	}

	/**
	 * Programaticaly inject dependencies. Inspired by https://community.jboss.org/thread/196807
	 *
	 * @param clazz
	 * @param injectionObject
	 * @throws NamingException
	 */
	public static void programmaticInjection(Class clazz, Object injectionObject) throws NamingException {
		InitialContext initialContext = new InitialContext();
		Object lookup = initialContext.lookup("java:comp/BeanManager");
		BeanManager beanManager = (BeanManager) lookup;
		AnnotatedType annotatedType = beanManager.createAnnotatedType(clazz);
		InjectionTarget injectionTarget = beanManager.createInjectionTarget(annotatedType);
		CreationalContext creationalContext = beanManager.createCreationalContext(null);
		injectionTarget.inject(injectionObject, creationalContext);
		creationalContext.release();
	}
}
