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

import java.util.logging.Logger;

import javax.inject.Inject;

import org.searchisko.api.util.SearchUtils;

/**
 * Base for mappers with some common methods.
 */
public class ExceptionMapperBase {

	@Inject
	protected Logger log;

	/**
	 * Collects all messages from exception and all causes from {@link Throwable#getCause()} nesting.
	 * 
	 * @param exception to collect messages from
	 * @return collected messages, never null but may be empty
	 */
	protected static CharSequence collectErrorMessages(Throwable exception) {
		StringBuilder messages = new StringBuilder();
		collectErrorMessages(messages, exception);
		return messages;
	}

	private static void collectErrorMessages(StringBuilder messages, Throwable e) {
		if (e != null) {
			String msg = e.getMessage();
			if (msg == null && e instanceof NullPointerException) {
				msg = "NullPointerException";
			}
			if (SearchUtils.trimToNull(msg) != null) {
				if (messages.length() > 0)
					messages.append(". Caused by: ");
				messages.append(msg);
			}
			collectErrorMessages(messages, e.getCause());
		}
	}

}
