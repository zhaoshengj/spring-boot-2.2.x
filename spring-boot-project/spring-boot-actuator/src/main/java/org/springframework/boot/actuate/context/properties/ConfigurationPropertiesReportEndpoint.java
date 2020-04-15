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

package org.springframework.boot.actuate.context.properties;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.endpoint.Sanitizer;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Endpoint @Endpoint} to expose application properties from
 * {@link ConfigurationProperties @ConfigurationProperties} annotated beans.
 *
 * <p>
 * To protect sensitive information from being exposed, certain property values are masked
 * if their names end with a set of configurable values (default "password" and "secret").
 * Configure property names by using {@code endpoints.configprops.keys_to_sanitize} in
 * your Spring Boot application configuration.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Endpoint(id = "configprops")
public class ConfigurationPropertiesReportEndpoint implements ApplicationContextAware {

	private static final String CONFIGURATION_PROPERTIES_FILTER_ID = "configurationPropertiesFilter";

	private final Sanitizer sanitizer = new Sanitizer();

	private ApplicationContext context;

	private ObjectMapper objectMapper;

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	public void setKeysToSanitize(String... keysToSanitize) {
		this.sanitizer.setKeysToSanitize(keysToSanitize);
	}

	@ReadOperation
	public ApplicationConfigurationProperties configurationProperties() {
		return extract(this.context);
	}

	private ApplicationConfigurationProperties extract(ApplicationContext context) {
		Map<String, ContextConfigurationProperties> contextProperties = new HashMap<>();
		ApplicationContext target = context;
		while (target != null) {
			contextProperties.put(target.getId(), describeConfigurationProperties(target, getObjectMapper()));
			target = target.getParent();
		}
		return new ApplicationConfigurationProperties(contextProperties);
	}

	private ContextConfigurationProperties describeConfigurationProperties(ApplicationContext context,
			ObjectMapper mapper) {
		Map<String, ConfigurationPropertiesBean> beans = ConfigurationPropertiesBean.getAll(context);
		Map<String, ConfigurationPropertiesBeanDescriptor> descriptors = new HashMap<>();
		beans.forEach((beanName, bean) -> {
			String prefix = bean.getAnnotation().prefix();
			descriptors.put(beanName, new ConfigurationPropertiesBeanDescriptor(prefix,
					sanitize(prefix, safeSerialize(mapper, bean.getInstance(), prefix))));
		});
		return new ContextConfigurationProperties(descriptors,
				(context.getParent() != null) ? context.getParent().getId() : null);
	}

	/**
	 * Cautiously serialize the bean to a map (returning a map with an error message
	 * instead of throwing an exception if there is a problem).
	 * @param mapper the object mapper
	 * @param bean the source bean
	 * @param prefix the prefix
	 * @return the serialized instance
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> safeSerialize(ObjectMapper mapper, Object bean, String prefix) {
		try {
			return new HashMap<>(mapper.convertValue(bean, Map.class));
		}
		catch (Exception ex) {
			return new HashMap<>(Collections.singletonMap("error", "Cannot serialize '" + prefix + "'"));
		}
	}

	/**
	 * Configure Jackson's {@link ObjectMapper} to be used to serialize the
	 * {@link ConfigurationProperties @ConfigurationProperties} objects into a {@link Map}
	 * structure.
	 * @param mapper the object mapper
	 */
	protected void configureObjectMapper(ObjectMapper mapper) {
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
		mapper.configure(MapperFeature.USE_STD_BEAN_NAMING, true);
		mapper.setSerializationInclusion(Include.NON_NULL);
		applyConfigurationPropertiesFilter(mapper);
		applySerializationModifier(mapper);
		mapper.registerModule(new JavaTimeModule());
	}

	private ObjectMapper getObjectMapper() {
		if (this.objectMapper == null) {
			this.objectMapper = new ObjectMapper();
			configureObjectMapper(this.objectMapper);
		}
		return this.objectMapper;
	}

	/**
	 * Ensure only bindable and non-cyclic bean properties are reported.
	 * @param mapper the object mapper
	 */
	private void applySerializationModifier(ObjectMapper mapper) {
		SerializerFactory factory = BeanSerializerFactory.instance
				.withSerializerModifier(new GenericSerializerModifier());
		mapper.setSerializerFactory(factory);
	}

	private void applyConfigurationPropertiesFilter(ObjectMapper mapper) {
		mapper.setAnnotationIntrospector(new ConfigurationPropertiesAnnotationIntrospector());
		mapper.setFilterProvider(
				new SimpleFilterProvider().setDefaultFilter(new ConfigurationPropertiesPropertyFilter()));
	}

	/**
	 * Sanitize all unwanted configuration properties to avoid leaking of sensitive
	 * information.
	 * @param prefix the property prefix
	 * @param map the source map
	 * @return the sanitized map
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> sanitize(String prefix, Map<String, Object> map) {
		map.forEach((key, value) -> {
			String qualifiedKey = (prefix.isEmpty() ? prefix : prefix + ".") + key;
			if (value instanceof Map) {
				map.put(key, sanitize(qualifiedKey, (Map<String, Object>) value));
			}
			else if (value instanceof List) {
				map.put(key, sanitize(qualifiedKey, (List<Object>) value));
			}
			else {
				value = this.sanitizer.sanitize(key, value);
				value = this.sanitizer.sanitize(qualifiedKey, value);
				map.put(key, value);
			}
		});
		return map;
	}

	@SuppressWarnings("unchecked")
	private List<Object> sanitize(String prefix, List<Object> list) {
		List<Object> sanitized = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map) {
				sanitized.add(sanitize(prefix, (Map<String, Object>) item));
			}
			else if (item instanceof List) {
				sanitized.add(sanitize(prefix, (List<Object>) item));
			}
			else {
				sanitized.add(this.sanitizer.sanitize(prefix, item));
			}
		}
		return sanitized;
	}

	/**
	 * Extension to {@link JacksonAnnotationIntrospector} to suppress CGLIB generated bean
	 * properties.
	 */
	@SuppressWarnings("serial")
	private static class ConfigurationPropertiesAnnotationIntrospector extends JacksonAnnotationIntrospector {

		@Override
		public Object findFilterId(Annotated a) {
			Object id = super.findFilterId(a);
			if (id == null) {
				id = CONFIGURATION_PROPERTIES_FILTER_ID;
			}
			return id;
		}

	}

	/**
	 * {@link SimpleBeanPropertyFilter} for serialization of
	 * {@link ConfigurationProperties @ConfigurationProperties} beans. The filter hides:
	 *
	 * <ul>
	 * <li>Properties that have a name starting with '$$'.
	 * <li>Properties that are self-referential.
	 * <li>Properties that throw an exception when retrieving their value.
	 * </ul>
	 */
	private static class ConfigurationPropertiesPropertyFilter extends SimpleBeanPropertyFilter {

		private static final Log logger = LogFactory.getLog(ConfigurationPropertiesPropertyFilter.class);

		@Override
		protected boolean include(BeanPropertyWriter writer) {
			return include(writer.getFullName().getSimpleName());
		}

		@Override
		protected boolean include(PropertyWriter writer) {
			return include(writer.getFullName().getSimpleName());
		}

		private boolean include(String name) {
			return !name.startsWith("$$");
		}

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider,
				PropertyWriter writer) throws Exception {
			if (writer instanceof BeanPropertyWriter) {
				try {
					if (pojo == ((BeanPropertyWriter) writer).get(pojo)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Skipping '" + writer.getFullName() + "' on '" + pojo.getClass().getName()
									+ "' as it is self-referential");
						}
						return;
					}
				}
				catch (Exception ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping '" + writer.getFullName() + "' on '" + pojo.getClass().getName()
								+ "' as an exception was thrown when retrieving its value", ex);
					}
					return;
				}
			}
			super.serializeAsField(pojo, jgen, provider, writer);
		}

	}

	/**
	 * {@link BeanSerializerModifier} to return only relevant configuration properties.
	 */
	protected static class GenericSerializerModifier extends BeanSerializerModifier {

		@Override
		public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
				List<BeanPropertyWriter> beanProperties) {
			List<BeanPropertyWriter> result = new ArrayList<>();
			Class<?> beanClass = beanDesc.getType().getRawClass();
			Constructor<?> bindConstructor = findBindConstructor(ClassUtils.getUserClass(beanClass));
			for (BeanPropertyWriter writer : beanProperties) {
				if (isCandidate(beanDesc, writer, bindConstructor)) {
					result.add(writer);
				}
			}
			return result;
		}

		private boolean isCandidate(BeanDescription beanDesc, BeanPropertyWriter writer,
				Constructor<?> bindConstructor) {
			if (bindConstructor != null) {
				return Arrays.stream(bindConstructor.getParameters())
						.anyMatch((parameter) -> parameter.getName().equals(writer.getName()));
			}
			else {
				return isReadable(beanDesc, writer);
			}
		}

		private boolean isReadable(BeanDescription beanDesc, BeanPropertyWriter writer) {
			Class<?> parentType = beanDesc.getType().getRawClass();
			Class<?> type = writer.getType().getRawClass();
			AnnotatedMethod setter = findSetter(beanDesc, writer);
			// If there's a setter, we assume it's OK to report on the value,
			// similarly, if there's no setter but the package names match, we assume
			// that its a nested class used solely for binding to config props, so it
			// should be kosher. Lists and Maps are also auto-detected by default since
			// that's what the metadata generator does. This filter is not used if there
			// is JSON metadata for the property, so it's mainly for user-defined beans.
			return (setter != null) || ClassUtils.getPackageName(parentType).equals(ClassUtils.getPackageName(type))
					|| Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type);
		}

		private AnnotatedMethod findSetter(BeanDescription beanDesc, BeanPropertyWriter writer) {
			String name = "set" + determineAccessorSuffix(writer.getName());
			Class<?> type = writer.getType().getRawClass();
			AnnotatedMethod setter = beanDesc.findMethod(name, new Class<?>[] { type });
			// The enabled property of endpoints returns a boolean primitive but is set
			// using a Boolean class
			if (setter == null && type.equals(Boolean.TYPE)) {
				setter = beanDesc.findMethod(name, new Class<?>[] { Boolean.class });
			}
			return setter;
		}

		/**
		 * Determine the accessor suffix of the specified {@code propertyName}, see
		 * section 8.8 "Capitalization of inferred names" of the JavaBean specs for more
		 * details.
		 * @param propertyName the property name to turn into an accessor suffix
		 * @return the accessor suffix for {@code propertyName}
		 */
		private String determineAccessorSuffix(String propertyName) {
			if (propertyName.length() > 1 && Character.isUpperCase(propertyName.charAt(1))) {
				return propertyName;
			}
			return StringUtils.capitalize(propertyName);
		}

		private Constructor<?> findBindConstructor(Class<?> type) {
			boolean classConstructorBinding = MergedAnnotations
					.from(type, SearchStrategy.TYPE_HIERARCHY_AND_ENCLOSING_CLASSES)
					.isPresent(ConstructorBinding.class);
			if (KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type)) {
				Constructor<?> constructor = BeanUtils.findPrimaryConstructor(type);
				if (constructor != null) {
					return findBindConstructor(classConstructorBinding, constructor);
				}
			}
			return findBindConstructor(classConstructorBinding, type.getDeclaredConstructors());
		}

		private Constructor<?> findBindConstructor(boolean classConstructorBinding, Constructor<?>... candidates) {
			List<Constructor<?>> candidateConstructors = Arrays.stream(candidates)
					.filter((constructor) -> constructor.getParameterCount() > 0).collect(Collectors.toList());
			List<Constructor<?>> flaggedConstructors = candidateConstructors.stream()
					.filter((candidate) -> MergedAnnotations.from(candidate).isPresent(ConstructorBinding.class))
					.collect(Collectors.toList());
			if (flaggedConstructors.size() == 1) {
				return flaggedConstructors.get(0);
			}
			if (classConstructorBinding && candidateConstructors.size() == 1) {
				return candidateConstructors.get(0);
			}
			return null;
		}

	}

	/**
	 * A description of an application's
	 * {@link ConfigurationProperties @ConfigurationProperties} beans. Primarily intended
	 * for serialization to JSON.
	 */
	public static final class ApplicationConfigurationProperties {

		private final Map<String, ContextConfigurationProperties> contexts;

		private ApplicationConfigurationProperties(Map<String, ContextConfigurationProperties> contexts) {
			this.contexts = contexts;
		}

		public Map<String, ContextConfigurationProperties> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * A description of an application context's
	 * {@link ConfigurationProperties @ConfigurationProperties} beans. Primarily intended
	 * for serialization to JSON.
	 */
	public static final class ContextConfigurationProperties {

		private final Map<String, ConfigurationPropertiesBeanDescriptor> beans;

		private final String parentId;

		private ContextConfigurationProperties(Map<String, ConfigurationPropertiesBeanDescriptor> beans,
				String parentId) {
			this.beans = beans;
			this.parentId = parentId;
		}

		public Map<String, ConfigurationPropertiesBeanDescriptor> getBeans() {
			return this.beans;
		}

		public String getParentId() {
			return this.parentId;
		}

	}

	/**
	 * A description of a {@link ConfigurationProperties @ConfigurationProperties} bean.
	 * Primarily intended for serialization to JSON.
	 */
	public static final class ConfigurationPropertiesBeanDescriptor {

		private final String prefix;

		private final Map<String, Object> properties;

		private ConfigurationPropertiesBeanDescriptor(String prefix, Map<String, Object> properties) {
			this.prefix = prefix;
			this.properties = properties;
		}

		public String getPrefix() {
			return this.prefix;
		}

		public Map<String, Object> getProperties() {
			return this.properties;
		}

	}

}
