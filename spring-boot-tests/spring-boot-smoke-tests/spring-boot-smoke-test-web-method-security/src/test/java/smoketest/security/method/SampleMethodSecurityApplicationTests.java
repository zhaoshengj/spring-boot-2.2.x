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

package smoketest.security.method;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SampleMethodSecurityApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testHome() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		ResponseEntity<String> entity = this.restTemplate.exchange("/", HttpMethod.GET, new HttpEntity<Void>(headers),
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("<title>Login");
	}

	@Test
	void testLogin() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.set("username", "admin");
		form.set("password", "admin");
		getCsrf(form, headers);
		ResponseEntity<String> entity = this.restTemplate.exchange("/login", HttpMethod.POST,
				new HttpEntity<>(form, headers), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(entity.getHeaders().getLocation().toString()).isEqualTo("http://localhost:" + this.port + "/");
	}

	@Test
	void testDenied() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.set("username", "user");
		form.set("password", "user");
		getCsrf(form, headers);
		ResponseEntity<String> entity = this.restTemplate.exchange("/login", HttpMethod.POST,
				new HttpEntity<>(form, headers), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		String cookie = entity.getHeaders().getFirst("Set-Cookie");
		headers.set("Cookie", cookie);
		ResponseEntity<String> page = this.restTemplate.exchange(entity.getHeaders().getLocation(), HttpMethod.GET,
				new HttpEntity<Void>(headers), String.class);
		assertThat(page.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(page.getBody()).contains("Access denied");
	}

	@Test
	void testManagementProtected() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		ResponseEntity<String> entity = this.restTemplate.exchange("/actuator/beans", HttpMethod.GET,
				new HttpEntity<Void>(headers), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void testManagementAuthorizedAccess() {
		BasicAuthenticationInterceptor basicAuthInterceptor = new BasicAuthenticationInterceptor("admin", "admin");
		this.restTemplate.getRestTemplate().getInterceptors().add(basicAuthInterceptor);
		try {
			ResponseEntity<String> entity = this.restTemplate.getForEntity("/actuator/beans", String.class);
			assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
		finally {
			this.restTemplate.getRestTemplate().getInterceptors().remove(basicAuthInterceptor);
		}
	}

	private void getCsrf(MultiValueMap<String, String> form, HttpHeaders headers) {
		ResponseEntity<String> page = this.restTemplate.getForEntity("/login", String.class);
		String cookie = page.getHeaders().getFirst("Set-Cookie");
		headers.set("Cookie", cookie);
		String body = page.getBody();
		Matcher matcher = Pattern.compile("(?s).*name=\"_csrf\".*?value=\"([^\"]+).*").matcher(body);
		matcher.find();
		form.set("_csrf", matcher.group(1));
	}

}
