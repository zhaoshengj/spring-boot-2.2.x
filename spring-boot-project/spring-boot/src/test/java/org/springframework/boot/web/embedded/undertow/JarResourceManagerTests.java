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

package org.springframework.boot.web.embedded.undertow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarResourceManager}.
 *
 * @author Andy Wilkinson
 */
class JarResourceManagerTests {

	private ResourceManager resourceManager;

	@BeforeEach
	void createJar(@TempDir File tempDir) throws IOException {
		File jar = new File(tempDir, "test.jar");
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
			out.putNextEntry(new ZipEntry("hello.txt"));
			out.write("hello".getBytes());
			out.close();
		}
		this.resourceManager = new JarResourceManager(jar);
	}

	@Test
	void emptyPathIsHandledCorrectly() throws IOException {
		Resource resource = this.resourceManager.getResource("");
		assertThat(resource).isNotNull();
		assertThat(resource.isDirectory()).isTrue();
	}

	@Test
	void rootPathIsHandledCorrectly() throws IOException {
		Resource resource = this.resourceManager.getResource("/");
		assertThat(resource).isNotNull();
		assertThat(resource.isDirectory()).isTrue();
	}

	@Test
	void resourceIsFoundInJarFile() throws IOException {
		Resource resource = this.resourceManager.getResource("/hello.txt");
		assertThat(resource).isNotNull();
		assertThat(resource.isDirectory()).isFalse();
		assertThat(resource.getContentLength()).isEqualTo(5);
	}

	@Test
	void resourceIsFoundInJarFileWithoutLeadingSlash() throws IOException {
		Resource resource = this.resourceManager.getResource("hello.txt");
		assertThat(resource).isNotNull();
		assertThat(resource.isDirectory()).isFalse();
		assertThat(resource.getContentLength()).isEqualTo(5);
	}

}
