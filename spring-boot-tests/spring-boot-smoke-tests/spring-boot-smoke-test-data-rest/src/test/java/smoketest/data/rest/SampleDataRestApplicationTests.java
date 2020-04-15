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

package smoketest.data.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test to run the application.
 *
 * @author Oliver Gierke
 * @author Andy Wilkinson
 */
@SpringBootTest
// Separate profile for web tests to avoid clashing databases
class SampleDataRestApplicationTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	void testHome() throws Exception {
		this.mvc.perform(get("/api")).andExpect(status().isOk()).andExpect(content().string(containsString("hotels")));
	}

	@Test
	void findByNameAndCountry() throws Exception {
		this.mvc.perform(get("/api/cities/search/findByNameAndCountryAllIgnoringCase?name=Melbourne&country=Australia"))
				.andExpect(status().isOk()).andExpect(jsonPath("state", equalTo("Victoria")))
				.andExpect(jsonPath("name", equalTo("Melbourne")));
	}

	@Test
	void findByContaining() throws Exception {
		this.mvc.perform(
				get("/api/cities/search/findByNameContainingAndCountryContainingAllIgnoringCase?name=&country=UK"))
				.andExpect(status().isOk()).andExpect(jsonPath("_embedded.cities", hasSize(3)));
	}

}
