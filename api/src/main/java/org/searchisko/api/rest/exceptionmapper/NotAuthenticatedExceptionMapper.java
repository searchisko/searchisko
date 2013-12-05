/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.searchisko.api.rest.exceptionmapper;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.searchisko.api.rest.exception.NotAuthenticatedException;

/**
 * Mapper for the {@link NotAuthenticatedException}.
 */
@Provider
public class NotAuthenticatedExceptionMapper implements ExceptionMapper<NotAuthenticatedException> {
	@Inject
	protected Logger log;

	@Override
	public Response toResponse(NotAuthenticatedException exception) {
		if (log.isLoggable(Level.WARNING)) {
			log.log(
					Level.WARNING,
					"Required authentication of '"
							+ exception.getMessage()
							+ "' user type. This should never happen. Check security interceprots configuration and use for your REST operation handler!",
					exception);
		}

		return Response.status(Response.Status.FORBIDDEN)
				.entity(MessageFormat.format("Required authentication of '{0}' user type.", exception.getMessage())).build();
	}
}
