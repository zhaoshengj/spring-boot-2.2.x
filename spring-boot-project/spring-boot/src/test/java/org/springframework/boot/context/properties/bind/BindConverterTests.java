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

package org.springframework.boot.context.properties.bind;

import java.beans.PropertyEditorSupport;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link BindConverter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class BindConverterTests {

	@Mock
	private Consumer<PropertyEditorRegistry> propertyEditorInitializer;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void createWhenConversionServiceIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> BindConverter.get(null, null))
				.withMessageContaining("ConversionService must not be null");
	}

	@Test
	void createWhenPropertyEditorInitializerIsNullShouldCreate() {
		BindConverter.get(ApplicationConversionService.getSharedInstance(), null);
	}

	@Test
	void createWhenPropertyEditorInitializerIsNotNullShouldUseToInitialize() {
		BindConverter.get(ApplicationConversionService.getSharedInstance(), this.propertyEditorInitializer);
		verify(this.propertyEditorInitializer).accept(any(PropertyEditorRegistry.class));
	}

	@Test
	void canConvertWhenHasDefaultEditorShouldReturnTrue() {
		BindConverter bindConverter = getPropertyEditorOnlyBindConverter(null);
		assertThat(bindConverter.canConvert("java.lang.RuntimeException", ResolvableType.forClass(Class.class)))
				.isTrue();
	}

	@Test
	void canConvertWhenHasCustomEditorShouldReturnTrue() {
		BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
		assertThat(bindConverter.canConvert("test", ResolvableType.forClass(SampleType.class))).isTrue();
	}

	@Test
	void canConvertWhenHasEditorByConventionShouldReturnTrue() {
		BindConverter bindConverter = getPropertyEditorOnlyBindConverter(null);
		assertThat(bindConverter.canConvert("test", ResolvableType.forClass(ConventionType.class))).isTrue();
	}

	@Test
	void canConvertWhenHasEditorForCollectionElementShouldReturnTrue() {
		BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
		assertThat(bindConverter.canConvert("test", ResolvableType.forClassWithGenerics(List.class, SampleType.class)))
				.isTrue();
	}

	@Test
	void canConvertWhenHasEditorForArrayElementShouldReturnTrue() {
		BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
		assertThat(bindConverter.canConvert("test", ResolvableType.forClass(SampleType[].class))).isTrue();
	}

	@Test
	void canConvertWhenConversionServiceCanConvertShouldReturnTrue() {
		BindConverter bindConverter = getBindConverter(new SampleTypeConverter());
		assertThat(bindConverter.canConvert("test", ResolvableType.forClass(SampleType.class))).isTrue();
	}

	@Test
	void canConvertWhenNotPropertyEditorAndConversionServiceCannotConvertShouldReturnFalse() {
		BindConverter bindConverter = BindConverter.get(ApplicationConversionService.getSharedInstance(), null);
		assertThat(bindConverter.canConvert("test", ResolvableType.forClass(SampleType.class))).isFalse();
	}

	@Test
	void convertWhenHasDefaultEditorShouldConvert() {
		BindConverter bindConverter = getPropertyEditorOnlyBindConverter(null);
		Class<?> converted = bindConverter.convert("java.lang.RuntimeException", ResolvableType.forClass(Class.class));
		assertThat(converted).isEqualTo(RuntimeException.class);
	}

	@Test
	void convertWhenHasCustomEditorShouldConvert() {
		BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
		SampleType converted = bindConverter.convert("test", ResolvableType.forClass(SampleType.class));
		assertThat(converted.getText()).isEqualTo("test");
	}

	@Test
	void convertWhenHasEditorByConventionShouldConvert() {
		BindConverter bindConverter = getPropertyEditorOnlyBindConverter(null);
		ConventionType converted = bindConverter.convert("test", ResolvableType.forClass(ConventionType.class));
		assertThat(converted.getText()).isEqualTo("test");
	}

	@Test
	void convertWhenHasEditorForCollectionElementShouldConvert() {
		BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
		List<SampleType> converted = bindConverter.convert("test",
				ResolvableType.forClassWithGenerics(List.class, SampleType.class));
		assertThat(converted).hasSize(1);
		assertThat(converted.get(0).getText()).isEqualTo("test");
	}

	@Test
	void convertWhenHasEditorForArrayElementShouldConvert() {
		BindConverter bindConverter = getPropertyEditorOnlyBindConverter(this::registerSampleTypeEditor);
		SampleType[] converted = bindConverter.convert("test", ResolvableType.forClass(SampleType[].class));
		assertThat(converted).isNotEmpty();
		assertThat(converted[0].getText()).isEqualTo("test");
	}

	@Test
	void convertWhenConversionServiceCanConvertShouldConvert() {
		BindConverter bindConverter = getBindConverter(new SampleTypeConverter());
		SampleType converted = bindConverter.convert("test", ResolvableType.forClass(SampleType.class));
		assertThat(converted.getText()).isEqualTo("test");
	}

	@Test
	void convertWhenNotPropertyEditorAndConversionServiceCannotConvertShouldThrowException() {
		BindConverter bindConverter = BindConverter.get(ApplicationConversionService.getSharedInstance(), null);
		assertThatExceptionOfType(ConverterNotFoundException.class)
				.isThrownBy(() -> bindConverter.convert("test", ResolvableType.forClass(SampleType.class)));
	}

	@Test
	void convertWhenConvertingToFileShouldExcludeFileEditor() {
		// For back compatibility we want true file conversion and not an accidental
		// classpath resource reference. See gh-12163
		BindConverter bindConverter = BindConverter.get(new GenericConversionService(), null);
		File result = bindConverter.convert(".", ResolvableType.forClass(File.class));
		assertThat(result.getPath()).isEqualTo(".");
	}

	@Test
	void fallsBackToApplicationConversionService() {
		BindConverter bindConverter = BindConverter.get(new GenericConversionService(), null);
		Duration result = bindConverter.convert("10s", ResolvableType.forClass(Duration.class));
		assertThat(result.getSeconds()).isEqualTo(10);
	}

	private BindConverter getPropertyEditorOnlyBindConverter(
			Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
		return BindConverter.get(new ThrowingConversionService(), propertyEditorInitializer);
	}

	private BindConverter getBindConverter(Converter<?, ?> converter) {
		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addConverter(converter);
		return BindConverter.get(conversionService, null);
	}

	private void registerSampleTypeEditor(PropertyEditorRegistry registry) {
		registry.registerCustomEditor(SampleType.class, new SampleTypePropertyEditor());
	}

	static class SampleType {

		private String text;

		String getText() {
			return this.text;
		}

	}

	static class SampleTypePropertyEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			SampleType value = new SampleType();
			value.text = text;
			setValue(value);
		}

	}

	static class SampleTypeConverter implements Converter<String, SampleType> {

		@Override
		public SampleType convert(String source) {
			SampleType result = new SampleType();
			result.text = source;
			return result;
		}

	}

	static class ConventionType {

		private String text;

		String getText() {
			return this.text;
		}

	}

	static class ConventionTypeEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			ConventionType value = new ConventionType();
			value.text = text;
			setValue(value);
		}

	}

	/**
	 * {@link ConversionService} that always throws an {@link AssertionError}.
	 */
	static class ThrowingConversionService implements ConversionService {

		@Override
		public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
			throw new AssertionError("Should not call conversion service");
		}

		@Override
		public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
			throw new AssertionError("Should not call conversion service");
		}

		@Override
		public <T> T convert(Object source, Class<T> targetType) {
			throw new AssertionError("Should not call conversion service");
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			throw new AssertionError("Should not call conversion service");
		}

	}

}
