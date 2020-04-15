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

package sample;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for {@link SampleGlassfishDeployApplication}.
 */
public class SampleGlassfishDeployApplicationIT {

	private int port = Integer.valueOf(System.getProperty("port"));

	@Test
	void testHome() throws Exception {
		String url = "http://localhost:" + this.port + "/bootapp/";
		System.out.println(url);
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(url, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

	@Test
	void testHealth() throws Exception {
		String url = "http://localhost:" + this.port + "/bootapp/actuator/health";
		System.out.println(url);
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(url, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("{\"status\":\"UP\"}");
	}

}
