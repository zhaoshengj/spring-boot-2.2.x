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

package org.springframework.boot.devtools.remote.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.core.Ordered;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link Dispatcher}.
 *
 * @author Phillip Webb
 */
class DispatcherTests {

	@Mock
	private AccessManager accessManager;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private ServerHttpRequest serverRequest;

	private ServerHttpResponse serverResponse;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		this.serverRequest = new ServletServerHttpRequest(this.request);
		this.serverResponse = new ServletServerHttpResponse(this.response);
	}

	@Test
	void accessManagerMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Dispatcher(null, Collections.emptyList()))
				.withMessageContaining("AccessManager must not be null");
	}

	@Test
	void mappersMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Dispatcher(this.accessManager, null))
				.withMessageContaining("Mappers must not be null");
	}

	@Test
	void accessManagerVetoRequest() throws Exception {
		given(this.accessManager.isAllowed(any(ServerHttpRequest.class))).willReturn(false);
		HandlerMapper mapper = mock(HandlerMapper.class);
		Handler handler = mock(Handler.class);
		given(mapper.getHandler(any(ServerHttpRequest.class))).willReturn(handler);
		Dispatcher dispatcher = new Dispatcher(this.accessManager, Collections.singleton(mapper));
		dispatcher.handle(this.serverRequest, this.serverResponse);
		verifyNoInteractions(handler);
		assertThat(this.response.getStatus()).isEqualTo(403);
	}

	@Test
	void accessManagerAllowRequest() throws Exception {
		given(this.accessManager.isAllowed(any(ServerHttpRequest.class))).willReturn(true);
		HandlerMapper mapper = mock(HandlerMapper.class);
		Handler handler = mock(Handler.class);
		given(mapper.getHandler(any(ServerHttpRequest.class))).willReturn(handler);
		Dispatcher dispatcher = new Dispatcher(this.accessManager, Collections.singleton(mapper));
		dispatcher.handle(this.serverRequest, this.serverResponse);
		verify(handler).handle(this.serverRequest, this.serverResponse);
	}

	@Test
	void ordersMappers() throws Exception {
		HandlerMapper mapper1 = mock(HandlerMapper.class, withSettings().extraInterfaces(Ordered.class));
		HandlerMapper mapper2 = mock(HandlerMapper.class, withSettings().extraInterfaces(Ordered.class));
		given(((Ordered) mapper1).getOrder()).willReturn(1);
		given(((Ordered) mapper2).getOrder()).willReturn(2);
		List<HandlerMapper> mappers = Arrays.asList(mapper2, mapper1);
		Dispatcher dispatcher = new Dispatcher(AccessManager.PERMIT_ALL, mappers);
		dispatcher.handle(this.serverRequest, this.serverResponse);
		InOrder inOrder = inOrder(mapper1, mapper2);
		inOrder.verify(mapper1).getHandler(this.serverRequest);
		inOrder.verify(mapper2).getHandler(this.serverRequest);
	}

}
