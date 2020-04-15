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

package smoketest.test.web;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import smoketest.test.WelcomeCommandLineRunner;
import smoketest.test.domain.VehicleIdentificationNumber;
import smoketest.test.service.VehicleDetails;
import smoketest.test.service.VehicleIdentificationNumberNotFoundException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} based tests for {@link UserVehicleController}.
 *
 * @author Phillip Webb
 */
@WebMvcTest(UserVehicleController.class)
class UserVehicleControllerTests {

	private static final VehicleIdentificationNumber VIN = new VehicleIdentificationNumber("00000000000000000");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ApplicationContext applicationContext;

	@MockBean
	private UserVehicleService userVehicleService;

	@Test
	void getVehicleWhenRequestingTextShouldReturnMakeAndModel() throws Exception {
		given(this.userVehicleService.getVehicleDetails("sboot")).willReturn(new VehicleDetails("Honda", "Civic"));
		this.mvc.perform(get("/sboot/vehicle").accept(MediaType.TEXT_PLAIN)).andExpect(status().isOk())
				.andExpect(content().string("Honda Civic"));
	}

	@Test
	void getVehicleWhenRequestingJsonShouldReturnMakeAndModel() throws Exception {
		given(this.userVehicleService.getVehicleDetails("sboot")).willReturn(new VehicleDetails("Honda", "Civic"));
		this.mvc.perform(get("/sboot/vehicle").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().json("{'make':'Honda','model':'Civic'}"));
	}

	@Test
	void getVehicleWhenRequestingHtmlShouldReturnMakeAndModel() throws Exception {
		given(this.userVehicleService.getVehicleDetails("sboot")).willReturn(new VehicleDetails("Honda", "Civic"));
		this.mvc.perform(get("/sboot/vehicle.html").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(content().string(containsString("<h1>Honda Civic</h1>")));
	}

	@Test
	void getVehicleWhenUserNotFoundShouldReturnNotFound() throws Exception {
		given(this.userVehicleService.getVehicleDetails("sboot")).willThrow(new UserNameNotFoundException("sboot"));
		this.mvc.perform(get("/sboot/vehicle")).andExpect(status().isNotFound());
	}

	@Test
	void getVehicleWhenVinNotFoundShouldReturnNotFound() throws Exception {
		given(this.userVehicleService.getVehicleDetails("sboot"))
				.willThrow(new VehicleIdentificationNumberNotFoundException(VIN));
		this.mvc.perform(get("/sboot/vehicle")).andExpect(status().isNotFound());
	}

	@Test
	void welcomeCommandLineRunnerShouldNotBeAvailable() {
		// Since we're a @WebMvcTest WelcomeCommandLineRunner should not be available.
		Assertions.assertThatThrownBy(() -> this.applicationContext.getBean(WelcomeCommandLineRunner.class))
				.isInstanceOf(NoSuchBeanDefinitionException.class);
	}

}
