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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.gradle.junit.GradleCompatibilityExtension;
import org.springframework.boot.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WarPluginAction}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(GradleCompatibilityExtension.class)
class JavaPluginActionIntegrationTests {

	GradleBuild gradleBuild;

	@TestTemplate
	void noBootJarTaskWithoutJavaPluginApplied() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=bootJar").getOutput())
				.contains("bootJar exists = false");
	}

	@TestTemplate
	void applyingJavaPluginCreatesBootJarTask() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=bootJar", "-PapplyJavaPlugin").getOutput())
				.contains("bootJar exists = true");
	}

	@TestTemplate
	void noBootRunTaskWithoutJavaPluginApplied() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=bootRun").getOutput())
				.contains("bootRun exists = false");
	}

	@TestTemplate
	void applyingJavaPluginCreatesBootRunTask() {
		assertThat(this.gradleBuild.build("taskExists", "-PtaskName=bootRun", "-PapplyJavaPlugin").getOutput())
				.contains("bootRun exists = true");
	}

	@TestTemplate
	void javaCompileTasksUseUtf8Encoding() {
		assertThat(this.gradleBuild.build("javaCompileEncoding", "-PapplyJavaPlugin").getOutput())
				.contains("compileJava = UTF-8").contains("compileTestJava = UTF-8");
	}

	@TestTemplate
	void javaCompileTasksUseParametersCompilerFlagByDefault() {
		assertThat(this.gradleBuild.build("javaCompileTasksCompilerArgs").getOutput())
				.contains("compileJava compiler args: [-parameters]")
				.contains("compileTestJava compiler args: [-parameters]");
	}

	@TestTemplate
	void javaCompileTasksUseParametersAndAdditionalCompilerFlags() {
		assertThat(this.gradleBuild.build("javaCompileTasksCompilerArgs").getOutput())
				.contains("compileJava compiler args: [-parameters, -Xlint:all]")
				.contains("compileTestJava compiler args: [-parameters, -Xlint:all]");
	}

	@TestTemplate
	void javaCompileTasksCanOverrideDefaultParametersCompilerFlag() {
		assertThat(this.gradleBuild.build("javaCompileTasksCompilerArgs").getOutput())
				.contains("compileJava compiler args: [-Xlint:all]")
				.contains("compileTestJava compiler args: [-Xlint:all]");
	}

	@TestTemplate
	void assembleRunsBootJarAndJarIsSkipped() {
		BuildResult result = this.gradleBuild.build("assemble");
		assertThat(result.task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.task(":jar").getOutcome()).isEqualTo(TaskOutcome.SKIPPED);
	}

	@TestTemplate
	void errorMessageIsHelpfulWhenMainClassCannotBeResolved() {
		BuildResult result = this.gradleBuild.buildAndFail("build", "-PapplyJavaPlugin");
		assertThat(result.task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.FAILED);
		assertThat(result.getOutput()).contains("Main class name has not been configured and it could not be resolved");
	}

	@TestTemplate
	void jarAndBootJarCanBothBeBuilt() {
		BuildResult result = this.gradleBuild.build("assemble");
		assertThat(result.task(":bootJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.task(":jar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
		assertThat(buildLibs.listFiles()).containsExactlyInAnyOrder(
				new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".jar"),
				new File(buildLibs, this.gradleBuild.getProjectDir().getName() + "-boot.jar"));
	}

	@TestTemplate
	void additionalMetadataLocationsConfiguredWhenProcessorIsPresent() throws IOException {
		createMinimalMainSource();
		File libs = new File(this.gradleBuild.getProjectDir(), "libs");
		libs.mkdirs();
		new JarOutputStream(new FileOutputStream(new File(libs, "spring-boot-configuration-processor-1.2.3.jar")))
				.close();
		BuildResult result = this.gradleBuild.build("compileJava");
		assertThat(result.task(":compileJava").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("compileJava compiler args: [-parameters, -Aorg.springframework.boot."
				+ "configurationprocessor.additionalMetadataLocations="
				+ new File(this.gradleBuild.getProjectDir(), "src/main/resources").getCanonicalPath());
	}

	@TestTemplate
	void additionalMetadataLocationsNotConfiguredWhenProcessorIsAbsent() throws IOException {
		createMinimalMainSource();
		BuildResult result = this.gradleBuild.build("compileJava");
		assertThat(result.task(":compileJava").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
		assertThat(result.getOutput()).contains("compileJava compiler args: [-parameters]");
	}

	private void createMinimalMainSource() throws IOException {
		File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/com/example");
		examplePackage.mkdirs();
		new File(examplePackage, "Application.java").createNewFile();
	}

}
