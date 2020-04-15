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

package org.springframework.boot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceBanner}.
 *
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Toshiaki Maki
 */
class ResourceBannerTests {

	@AfterEach
	void reset() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@Test
	void renderVersions() {
		Resource resource = new ByteArrayResource(
				"banner ${a} ${spring-boot.version} ${application.version}".getBytes());
		String banner = printBanner(resource, "10.2", "2.0", null);
		assertThat(banner).startsWith("banner 1 10.2 2.0");
	}

	@Test
	void renderWithoutVersions() {
		Resource resource = new ByteArrayResource(
				"banner ${a} ${spring-boot.version} ${application.version}".getBytes());
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("banner 1  ");
	}

	@Test
	void renderFormattedVersions() {
		Resource resource = new ByteArrayResource(
				"banner ${a}${spring-boot.formatted-version}${application.formatted-version}".getBytes());
		String banner = printBanner(resource, "10.2", "2.0", null);
		assertThat(banner).startsWith("banner 1 (v10.2) (v2.0)");
	}

	@Test
	void renderWithoutFormattedVersions() {
		Resource resource = new ByteArrayResource(
				"banner ${a}${spring-boot.formatted-version}${application.formatted-version}".getBytes());
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("banner 1");
	}

	@Test
	void renderWithColors() {
		Resource resource = new ByteArrayResource("${Ansi.RED}This is red.${Ansi.NORMAL}".getBytes());
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("\u001B[31mThis is red.\u001B[0m");
	}

	@Test
	void renderWithColorsButDisabled() {
		Resource resource = new ByteArrayResource("${Ansi.RED}This is red.${Ansi.NORMAL}".getBytes());
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("This is red.");
	}

	@Test
	void renderWith256Colors() {
		Resource resource = new ByteArrayResource("${AnsiColor.208}This is orange.${Ansi.NORMAL}".getBytes());
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("\033[38;5;208mThis is orange.\u001B[0m");
	}

	@Test
	void renderWith256ColorsButDisabled() {
		Resource resource = new ByteArrayResource("${AnsiColor.208}This is orange.${Ansi.NORMAL}".getBytes());
		AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("This is orange.");
	}

	@Test
	void renderWithTitle() {
		Resource resource = new ByteArrayResource("banner ${application.title} ${a}".getBytes());
		String banner = printBanner(resource, null, null, "title");
		assertThat(banner).startsWith("banner title 1");
	}

	@Test
	void renderWithoutTitle() {
		Resource resource = new ByteArrayResource("banner ${application.title} ${a}".getBytes());
		String banner = printBanner(resource, null, null, null);
		assertThat(banner).startsWith("banner  1");
	}

	private String printBanner(Resource resource, String bootVersion, String applicationVersion,
			String applicationTitle) {
		ResourceBanner banner = new MockResourceBanner(resource, bootVersion, applicationVersion, applicationTitle);
		ConfigurableEnvironment environment = new MockEnvironment();
		Map<String, Object> source = Collections.singletonMap("a", "1");
		environment.getPropertySources().addLast(new MapPropertySource("map", source));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		banner.printBanner(environment, getClass(), new PrintStream(out));
		return out.toString();
	}

	static class MockResourceBanner extends ResourceBanner {

		private final String bootVersion;

		private final String applicationVersion;

		private final String applicationTitle;

		MockResourceBanner(Resource resource, String bootVersion, String applicationVersion, String applicationTitle) {
			super(resource);
			this.bootVersion = bootVersion;
			this.applicationVersion = applicationVersion;
			this.applicationTitle = applicationTitle;
		}

		@Override
		protected String getBootVersion() {
			return this.bootVersion;
		}

		@Override
		protected String getApplicationVersion(Class<?> sourceClass) {
			return this.applicationVersion;
		}

		@Override
		protected String getApplicationTitle(Class<?> sourceClass) {
			return this.applicationTitle;
		}

	}

}
