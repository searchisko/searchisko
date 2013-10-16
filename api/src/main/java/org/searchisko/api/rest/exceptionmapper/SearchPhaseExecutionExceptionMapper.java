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

import org.elasticsearch.action.search.SearchPhaseExecutionException;

/**
 * A simple exception mapper for searching failures.
 */
@Provider
public class SearchPhaseExecutionExceptionMapper implements ExceptionMapper<SearchPhaseExecutionException> {
    @Inject
    protected Logger log;

    @Override
    public Response toResponse(SearchPhaseExecutionException exception) {
        if (log.isLoggable(Level.WARNING)) {
            log.log(Level.WARNING, "Exception {0} occurred. Message: {1}",
                    new Object[] { exception.getClass().getName(), exception.getMessage() });
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Exception trace.", exception);
            }
        }
        final Response.ResponseBuilder response = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        // TODO: We need to figure out which project stage we're in
        final StringBuilder exceptionMessage = new StringBuilder("An error occurred during your search.");
//        if (!production) {
            exceptionMessage.append(exception.getMessage());
//        } else {
//            response.entity("An error occurred during your search.");
//        }
        response.entity(exceptionMessage.toString());
        return response.build();
    }
}
