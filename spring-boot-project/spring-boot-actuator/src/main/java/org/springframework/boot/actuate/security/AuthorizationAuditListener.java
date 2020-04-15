/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.security.access.event.AbstractAuthorizationEvent;
import org.springframework.security.access.event.AuthenticationCredentialsNotFoundEvent;
import org.springframework.security.access.event.AuthorizationFailureEvent;

/**
 * Default implementation of {@link AbstractAuthorizationAuditListener}.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @since 1.0.0
 */
public class AuthorizationAuditListener extends AbstractAuthorizationAuditListener {

	/**
	 * Authorization failure event type.
	 */
	public static final String AUTHORIZATION_FAILURE = "AUTHORIZATION_FAILURE";

	@Override
	public void onApplicationEvent(AbstractAuthorizationEvent event) {
		if (event instanceof AuthenticationCredentialsNotFoundEvent) {
			onAuthenticationCredentialsNotFoundEvent((AuthenticationCredentialsNotFoundEvent) event);
		}
		else if (event instanceof AuthorizationFailureEvent) {
			onAuthorizationFailureEvent((AuthorizationFailureEvent) event);
		}
	}

	private void onAuthenticationCredentialsNotFoundEvent(AuthenticationCredentialsNotFoundEvent event) {
		Map<String, Object> data = new HashMap<>();
		data.put("type", event.getCredentialsNotFoundException().getClass().getName());
		data.put("message", event.getCredentialsNotFoundException().getMessage());
		publish(new AuditEvent("<unknown>", AuthenticationAuditListener.AUTHENTICATION_FAILURE, data));
	}

	private void onAuthorizationFailureEvent(AuthorizationFailureEvent event) {
		Map<String, Object> data = new HashMap<>();
		data.put("type", event.getAccessDeniedException().getClass().getName());
		data.put("message", event.getAccessDeniedException().getMessage());
		if (event.getAuthentication().getDetails() != null) {
			data.put("details", event.getAuthentication().getDetails());
		}
		publish(new AuditEvent(event.getAuthentication().getName(), AUTHORIZATION_FAILURE, data));
	}

}
