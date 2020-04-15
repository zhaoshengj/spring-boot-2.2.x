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

package org.springframework.boot.origin;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link PropertySourceOrigin}.
 *
 * @author Phillip Webb
 */
class PropertySourceOriginTests {

	@Test
	void createWhenPropertySourceIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PropertySourceOrigin(null, "name"))
				.withMessageContaining("PropertySource must not be null");
	}

	@Test
	void createWhenPropertyNameIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PropertySourceOrigin(mock(PropertySource.class), null))
				.withMessageContaining("PropertyName must not be empty");
	}

	@Test
	void createWhenPropertyNameIsEmptyShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PropertySourceOrigin(mock(PropertySource.class), ""))
				.withMessageContaining("PropertyName must not be empty");
	}

	@Test
	void getPropertySourceShouldReturnPropertySource() {
		MapPropertySource propertySource = new MapPropertySource("test", new HashMap<>());
		PropertySourceOrigin origin = new PropertySourceOrigin(propertySource, "foo");
		assertThat(origin.getPropertySource()).isEqualTo(propertySource);
	}

	@Test
	void getPropertyNameShouldReturnPropertyName() {
		MapPropertySource propertySource = new MapPropertySource("test", new HashMap<>());
		PropertySourceOrigin origin = new PropertySourceOrigin(propertySource, "foo");
		assertThat(origin.getPropertyName()).isEqualTo("foo");
	}

	@Test
	void toStringShouldShowDetails() {
		MapPropertySource propertySource = new MapPropertySource("test", new HashMap<>());
		PropertySourceOrigin origin = new PropertySourceOrigin(propertySource, "foo");
		assertThat(origin.toString()).isEqualTo("\"foo\" from property source \"test\"");
	}

	@Test
	@SuppressWarnings("unchecked")
	void getWhenPropertySourceSupportsOriginLookupShouldReturnOrigin() {
		Origin origin = mock(Origin.class);
		PropertySource<?> propertySource = mock(PropertySource.class,
				withSettings().extraInterfaces(OriginLookup.class));
		OriginLookup<String> originCapablePropertySource = (OriginLookup<String>) propertySource;
		given(originCapablePropertySource.getOrigin("foo")).willReturn(origin);
		assertThat(PropertySourceOrigin.get(propertySource, "foo")).isSameAs(origin);
	}

	@Test
	void getWhenPropertySourceSupportsOriginLookupButNoOriginShouldWrap() {
		PropertySource<?> propertySource = mock(PropertySource.class,
				withSettings().extraInterfaces(OriginLookup.class));
		assertThat(PropertySourceOrigin.get(propertySource, "foo")).isInstanceOf(PropertySourceOrigin.class);
	}

	@Test
	void getWhenPropertySourceIsNotOriginAwareShouldWrap() {
		MapPropertySource propertySource = new MapPropertySource("test", new HashMap<>());
		PropertySourceOrigin origin = new PropertySourceOrigin(propertySource, "foo");
		assertThat(origin.getPropertySource()).isEqualTo(propertySource);
		assertThat(origin.getPropertyName()).isEqualTo("foo");
	}

}
