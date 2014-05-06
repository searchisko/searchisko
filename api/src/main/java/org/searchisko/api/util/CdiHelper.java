package org.searchisko.api.util;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * CDI Helper utility
 * @author Libor Krzyzanek
 */
public class CdiHelper {

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
