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

package smoketest.secure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(classes = { SampleSecureApplication.class })
class SampleSecureApplicationTests {

	@Autowired
	private SampleService service;

	private Authentication authentication;

	@BeforeEach
	void init() {
		this.authentication = new UsernamePasswordAuthenticationToken("user", "password");
	}

	@AfterEach
	void close() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void secure() {
		assertThatExceptionOfType(AuthenticationException.class)
				.isThrownBy(() -> SampleSecureApplicationTests.this.service.secure());
	}

	@Test
	void authenticated() {
		SecurityContextHolder.getContext().setAuthentication(this.authentication);
		assertThat("Hello Security").isEqualTo(this.service.secure());
	}

	@Test
	void preauth() {
		SecurityContextHolder.getContext().setAuthentication(this.authentication);
		assertThat("Hello World").isEqualTo(this.service.authorized());
	}

	@Test
	void denied() {
		SecurityContextHolder.getContext().setAuthentication(this.authentication);
		assertThatExceptionOfType(AccessDeniedException.class)
				.isThrownBy(() -> SampleSecureApplicationTests.this.service.denied());
	}

}
