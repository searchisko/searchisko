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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.searchisko.api.rest.exception.PreprocessorInvalidDataException;

/**
 * Mapper for the {@link PreprocessorInvalidDataException}.
 */
@Provider
public class PreprocessorInvalidDataExceptionMapper implements ExceptionMapper<PreprocessorInvalidDataException> {
	@Inject
	protected Logger log;

	@Override
	public Response toResponse(PreprocessorInvalidDataException exception) {
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, exception.getMessage(), exception);
		}

		return Response.status(Response.Status.BAD_REQUEST).entity("Input data are invalid: " + exception.getMessage())
				.build();
	}
}
