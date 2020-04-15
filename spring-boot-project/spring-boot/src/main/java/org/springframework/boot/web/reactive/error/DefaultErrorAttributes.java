/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.reactive.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Default implementation of {@link ErrorAttributes}. Provides the following attributes
 * when possible:
 * <ul>
 * <li>timestamp - The time that the errors were extracted</li>
 * <li>status - The status code</li>
 * <li>error - The error reason</li>
 * <li>exception - The class name of the root exception (if configured)</li>
 * <li>message - The exception message</li>
 * <li>errors - Any {@link ObjectError}s from a {@link BindingResult} exception
 * <li>trace - The exception stack trace</li>
 * <li>path - The URL path when the exception was raised</li>
 * <li>requestId - Unique ID associated with the current request</li>
 * </ul>
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Michele Mancioppi
 * @since 2.0.0
 * @see ErrorAttributes
 */
public class DefaultErrorAttributes implements ErrorAttributes {

	private static final String ERROR_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

	private final boolean includeException;

	/**
	 * Create a new {@link DefaultErrorAttributes} instance that does not include the
	 * "exception" attribute.
	 */
	public DefaultErrorAttributes() {
		this(false);
	}

	/**
	 * Create a new {@link DefaultErrorAttributes} instance.
	 * @param includeException whether to include the "exception" attribute
	 */
	public DefaultErrorAttributes(boolean includeException) {
		this.includeException = includeException;
	}

	@Override
	public Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
		Map<String, Object> errorAttributes = new LinkedHashMap<>();
		errorAttributes.put("timestamp", new Date());
		errorAttributes.put("path", request.path());
		Throwable error = getError(request);
		MergedAnnotation<ResponseStatus> responseStatusAnnotation = MergedAnnotations
				.from(error.getClass(), SearchStrategy.TYPE_HIERARCHY).get(ResponseStatus.class);
		HttpStatus errorStatus = determineHttpStatus(error, responseStatusAnnotation);
		errorAttributes.put("status", errorStatus.value());
		errorAttributes.put("error", errorStatus.getReasonPhrase());
		errorAttributes.put("message", determineMessage(error, responseStatusAnnotation));
		errorAttributes.put("requestId", request.exchange().getRequest().getId());
		handleException(errorAttributes, determineException(error), includeStackTrace);
		return errorAttributes;
	}

	private HttpStatus determineHttpStatus(Throwable error, MergedAnnotation<ResponseStatus> responseStatusAnnotation) {
		if (error instanceof ResponseStatusException) {
			return ((ResponseStatusException) error).getStatus();
		}
		return responseStatusAnnotation.getValue("code", HttpStatus.class).orElse(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private String determineMessage(Throwable error, MergedAnnotation<ResponseStatus> responseStatusAnnotation) {
		if (error instanceof WebExchangeBindException) {
			return error.getMessage();
		}
		if (error instanceof ResponseStatusException) {
			return ((ResponseStatusException) error).getReason();
		}
		String reason = responseStatusAnnotation.getValue("reason", String.class).orElse("");
		if (StringUtils.hasText(reason)) {
			return reason;
		}
		return (error.getMessage() != null) ? error.getMessage() : "";
	}

	private Throwable determineException(Throwable error) {
		if (error instanceof ResponseStatusException) {
			return (error.getCause() != null) ? error.getCause() : error;
		}
		return error;
	}

	private void addStackTrace(Map<String, Object> errorAttributes, Throwable error) {
		StringWriter stackTrace = new StringWriter();
		error.printStackTrace(new PrintWriter(stackTrace));
		stackTrace.flush();
		errorAttributes.put("trace", stackTrace.toString());
	}

	private void handleException(Map<String, Object> errorAttributes, Throwable error, boolean includeStackTrace) {
		if (this.includeException) {
			errorAttributes.put("exception", error.getClass().getName());
		}
		if (includeStackTrace) {
			addStackTrace(errorAttributes, error);
		}
		if (error instanceof BindingResult) {
			BindingResult result = (BindingResult) error;
			if (result.hasErrors()) {
				errorAttributes.put("errors", result.getAllErrors());
			}
		}
	}

	@Override
	public Throwable getError(ServerRequest request) {
		return (Throwable) request.attribute(ERROR_ATTRIBUTE)
				.orElseThrow(() -> new IllegalStateException("Missing exception attribute in ServerWebExchange"));
	}

	@Override
	public void storeErrorInformation(Throwable error, ServerWebExchange exchange) {
		exchange.getAttributes().putIfAbsent(ERROR_ATTRIBUTE, error);
	}

}
