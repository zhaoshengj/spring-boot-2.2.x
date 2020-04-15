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

package org.springframework.boot.actuate.autoconfigure.metrics.export.dynatrace;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynatracePropertiesConfigAdapter}.
 *
 * @author Andy Wilkinson
 */
class DynatracePropertiesConfigAdapterTests {

	@Test
	void whenPropertiesUriIsSetAdapterUriReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setUri("https://dynatrace.example.com");
		assertThat(new DynatracePropertiesConfigAdapter(properties).uri()).isEqualTo("https://dynatrace.example.com");
	}

	@Test
	void whenPropertiesApiTokenIsSetAdapterApiTokenReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setApiToken("123ABC");
		assertThat(new DynatracePropertiesConfigAdapter(properties).apiToken()).isEqualTo("123ABC");
	}

	@Test
	void whenPropertiesDeviceIdIsSetAdapterDeviceIdReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setDeviceId("dev-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).deviceId()).isEqualTo("dev-1");
	}

	@Test
	void whenPropertiesTechnologyTypeIsSetAdapterTechnologyTypeReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setTechnologyType("tech-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).technologyType()).isEqualTo("tech-1");
	}

	@Test
	void whenPropertiesGroupIsSetAdapterGroupReturnsIt() {
		DynatraceProperties properties = new DynatraceProperties();
		properties.setGroup("group-1");
		assertThat(new DynatracePropertiesConfigAdapter(properties).group()).isEqualTo("group-1");
	}

}
