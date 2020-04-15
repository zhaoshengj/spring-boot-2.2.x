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

package org.springframework.boot.gradle.tasks.buildinfo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BuildInfo}.
 *
 * @author Andy Wilkinson
 */
class BuildInfoTests {

	@TempDir
	File temp;

	@Test
	void basicExecution() {
		Properties properties = buildInfoProperties(createTask(createProject("test")));
		assertThat(properties).containsKey("build.time");
		assertThat(properties).containsEntry("build.artifact", "unspecified");
		assertThat(properties).containsEntry("build.group", "");
		assertThat(properties).containsEntry("build.name", "test");
		assertThat(properties).containsEntry("build.version", "unspecified");
	}

	@Test
	void customArtifactIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setArtifact("custom");
		assertThat(buildInfoProperties(task)).containsEntry("build.artifact", "custom");
	}

	@Test
	void projectGroupIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProject().setGroup("com.example");
		assertThat(buildInfoProperties(task)).containsEntry("build.group", "com.example");
	}

	@Test
	void customGroupIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setGroup("com.example");
		assertThat(buildInfoProperties(task)).containsEntry("build.group", "com.example");
	}

	@Test
	void customNameIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setName("Example");
		assertThat(buildInfoProperties(task)).containsEntry("build.name", "Example");
	}

	@Test
	void projectVersionIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProject().setVersion("1.2.3");
		assertThat(buildInfoProperties(task)).containsEntry("build.version", "1.2.3");
	}

	@Test
	void customVersionIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setVersion("2.3.4");
		assertThat(buildInfoProperties(task)).containsEntry("build.version", "2.3.4");
	}

	@Test
	void timeIsSetInProperties() {
		BuildInfo task = createTask(createProject("test"));
		assertThat(buildInfoProperties(task)).containsEntry("build.time",
				DateTimeFormatter.ISO_INSTANT.format(task.getProperties().getTime()));
	}

	@Test
	void timeCanBeRemovedFromProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setTime(null);
		assertThat(buildInfoProperties(task)).doesNotContainKey("build.time");
	}

	@Test
	void timeCanBeCustomizedInProperties() {
		Instant now = Instant.now();
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setTime(now);
		assertThat(buildInfoProperties(task)).containsEntry("build.time", DateTimeFormatter.ISO_INSTANT.format(now));
	}

	@Test
	void additionalPropertiesAreReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().getAdditional().put("a", "alpha");
		task.getProperties().getAdditional().put("b", "bravo");
		assertThat(buildInfoProperties(task)).containsEntry("build.a", "alpha");
		assertThat(buildInfoProperties(task)).containsEntry("build.b", "bravo");
	}

	private Project createProject(String projectName) {
		File projectDir = new File(this.temp, projectName);
		return ProjectBuilder.builder().withProjectDir(projectDir).withName(projectName).build();
	}

	private BuildInfo createTask(Project project) {
		return project.getTasks().create("testBuildInfo", BuildInfo.class);
	}

	private Properties buildInfoProperties(BuildInfo task) {
		task.generateBuildProperties();
		return buildInfoProperties(new File(task.getDestinationDir(), "build-info.properties"));
	}

	private Properties buildInfoProperties(File file) {
		assertThat(file).isFile();
		Properties properties = new Properties();
		try (FileReader reader = new FileReader(file)) {
			properties.load(reader);
			return properties;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
