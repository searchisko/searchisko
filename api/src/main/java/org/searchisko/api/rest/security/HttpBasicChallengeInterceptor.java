package org.searchisko.api.rest.security;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.HeaderDecoratorPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.interceptors.SecurityInterceptor;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.searchisko.api.security.Role;

/**
 * Interceptor extends SecurityInterceptor for HTTP Basic Authentication Challenge headers for defined roles
 *
 * @author Libor Krzyzanek
 */
@Provider
@ServerInterceptor
@HeaderDecoratorPrecedence
public class HttpBasicChallengeInterceptor extends SecurityInterceptor {

	@Inject
	protected Logger log;

	/**
	 * List of tested roles
	 */
	public static final Set<String> testedRoles = new HashSet<>(Arrays.asList(Role.ALL_ROLES));

	static {
		// Contributor has different auth. mechanism
		testedRoles.remove(Role.CONTRIBUTOR);
	}

	public static final String CHALLENGE_TEXT = "Insert Provider's username and password";

	/**
	 * Creates standard Response with Http Basic authentication challenge
	 *
	 * @return
	 */
	public ServerResponse createHttpBasicChallengeResponse() {
		log.fine("REST Security, request is not authenticated, returning HTTP Basic Challenge response");
		ServerResponse response = new ServerResponse();
		response.setStatus(HttpResponseCodes.SC_UNAUTHORIZED);
		response.getMetadata().add("WWW-Authenticate", "Basic realm=\"" + CHALLENGE_TEXT + "\"");
		return response;
	}

	public ServerResponse createForbiddenResponse() {
		log.fine("REST Security, request is not authorized, returning 403");
		ServerResponse response = new ServerResponse();
		response.setStatus(HttpResponseCodes.SC_FORBIDDEN);
		return response;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public ServerResponse preProcess(HttpRequest request, ResourceMethod resourceMethod) throws Failure, WebApplicationException {
		// Retrieve again all annotations
		String[] rolesAllowed = null;
		boolean denyAll;
		boolean permitAll;

		Class declaring = resourceMethod.getResourceClass();
		Method method = resourceMethod.getMethod();

		if (declaring == null || method == null) {
			return null;
		}
		RolesAllowed allowed = (RolesAllowed) declaring.getAnnotation(RolesAllowed.class);
		RolesAllowed methodAllowed = method.getAnnotation(RolesAllowed.class);
		if (methodAllowed != null) {
			allowed = methodAllowed;
		}
		if (allowed != null) {
			rolesAllowed = allowed.value();
		}

		denyAll = (declaring.isAnnotationPresent(DenyAll.class)
				&& method.isAnnotationPresent(RolesAllowed.class) == false
				&& method.isAnnotationPresent(PermitAll.class) == false) || method.isAnnotationPresent(DenyAll.class);

		permitAll = (declaring.isAnnotationPresent(PermitAll.class) == true
				&& method.isAnnotationPresent(RolesAllowed.class) == false
				&& method.isAnnotationPresent(DenyAll.class) == false) || method.isAnnotationPresent(PermitAll.class);


		if (denyAll) {
			return createHttpBasicChallengeResponse();
		}
		if (permitAll) {
			return null;
		}
		if (rolesAllowed != null) {
			log.log(Level.FINEST, "Roles Allowed: {0}", rolesAllowed);
			SecurityContext context = ResteasyProviderFactory.getContextData(SecurityContext.class);
			if (context != null) {
				log.log(Level.FINEST, "User Principal: {0}", context.getUserPrincipal());
				boolean onlyTestedRole = true;
				for (String role : rolesAllowed) {
					if (context.isUserInRole(role)) {
						return null;
					}
					if (!testedRoles.contains(role)) {
						onlyTestedRole = false;
					}
				}
				if (onlyTestedRole) {
					if (context.getUserPrincipal() == null) {
						return createHttpBasicChallengeResponse();
					} else {
						return createForbiddenResponse();
					}
				} else {
					return super.preProcess(request, resourceMethod);
				}
			}
		}

		return null;
	}
}