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

package org.springframework.boot.gradle.plugin;

import java.io.File;
import java.io.IOException;

import org.gradle.api.Project;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.gradle.dsl.SpringBootExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MainClassConvention}.
 *
 * @author Andy Wilkinson
 */
class MainClassConventionTests {

	@TempDir
	File temp;

	private Project project;

	private MainClassConvention convention;

	@BeforeEach
	void createConvention() throws IOException {
		this.project = ProjectBuilder.builder().withProjectDir(this.temp).build();
		this.convention = new MainClassConvention(this.project, () -> null);
	}

	@Test
	void javaApplicationExtensionMainClassNameIsUsed() throws Exception {
		this.project.getPlugins().apply(ApplicationPlugin.class);
		JavaApplication extension = this.project.getExtensions().findByType(JavaApplication.class);
		extension.setMainClassName("com.example.MainClass");
		assertThat(this.convention.call()).isEqualTo("com.example.MainClass");
	}

	@Test
	void springBootExtensionMainClassNameIsUsed() throws Exception {
		SpringBootExtension extension = this.project.getExtensions().create("springBoot", SpringBootExtension.class,
				this.project);
		extension.setMainClassName("com.example.MainClass");
		assertThat(this.convention.call()).isEqualTo("com.example.MainClass");
	}

	@Test
	void springBootExtensionMainClassNameIsUsedInPreferenceToJavaApplicationExtensionMainClassName() throws Exception {
		this.project.getPlugins().apply(ApplicationPlugin.class);
		JavaApplication javaApplication = this.project.getExtensions().findByType(JavaApplication.class);
		javaApplication.setMainClassName("com.example.JavaApplicationMainClass");
		SpringBootExtension extension = this.project.getExtensions().create("springBoot", SpringBootExtension.class,
				this.project);
		extension.setMainClassName("com.example.SpringBootExtensionMainClass");
		assertThat(this.convention.call()).isEqualTo("com.example.SpringBootExtensionMainClass");
	}

}
