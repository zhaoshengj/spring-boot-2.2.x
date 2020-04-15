/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.properties.bind.validation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.DataObjectPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.ResolvableType;
import org.springframework.validation.AbstractBindingResult;
import org.springframework.validation.Validator;

/**
 * {@link BindHandler} to apply {@link Validator Validators} to bound results.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class ValidationBindHandler extends AbstractBindHandler {

	private final Validator[] validators;

	private final Map<ConfigurationPropertyName, ResolvableType> boundTypes = new LinkedHashMap<>();

	private final Map<ConfigurationPropertyName, Object> boundResults = new LinkedHashMap<>();

	private final Set<ConfigurationProperty> boundProperties = new LinkedHashSet<>();

	private BindValidationException exception;

	public ValidationBindHandler(Validator... validators) {
		this.validators = validators;
	}

	public ValidationBindHandler(BindHandler parent, Validator... validators) {
		super(parent);
		this.validators = validators;
	}

	@Override
	public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
		this.boundTypes.put(name, target.getType());
		return super.onStart(name, target, context);
	}

	@Override
	public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		this.boundResults.put(name, result);
		if (context.getConfigurationProperty() != null) {
			this.boundProperties.add(context.getConfigurationProperty());
		}
		return super.onSuccess(name, target, context, result);
	}

	@Override
	public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error)
			throws Exception {
		Object result = super.onFailure(name, target, context, error);
		if (result != null) {
			clear();
			this.boundResults.put(name, result);
		}
		validate(name, target, context, result);
		return result;
	}

	private void clear() {
		this.boundTypes.clear();
		this.boundResults.clear();
		this.boundProperties.clear();
		this.exception = null;
	}

	@Override
	public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result)
			throws Exception {
		validate(name, target, context, result);
		super.onFinish(name, target, context, result);
	}

	private void validate(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		if (this.exception == null) {
			Object validationTarget = getValidationTarget(target, context, result);
			Class<?> validationType = target.getBoxedType().resolve();
			if (validationTarget != null) {
				validateAndPush(name, validationTarget, validationType);
			}
		}
		if (context.getDepth() == 0 && this.exception != null) {
			throw this.exception;
		}
	}

	private Object getValidationTarget(Bindable<?> target, BindContext context, Object result) {
		if (result != null) {
			return result;
		}
		if (context.getDepth() == 0 && target.getValue() != null) {
			return target.getValue().get();
		}
		return null;
	}

	private void validateAndPush(ConfigurationPropertyName name, Object target, Class<?> type) {
		ValidationResult result = null;
		for (Validator validator : this.validators) {
			if (validator.supports(type)) {
				result = (result != null) ? result : new ValidationResult(name, target);
				validator.validate(target, result);
			}
		}
		if (result != null && result.hasErrors()) {
			this.exception = new BindValidationException(result.getValidationErrors());
		}
	}

	/**
	 * {@link AbstractBindingResult} implementation backed by the bound properties.
	 */
	private class ValidationResult extends AbstractBindingResult {

		private final ConfigurationPropertyName name;

		private Object target;

		protected ValidationResult(ConfigurationPropertyName name, Object target) {
			super(null);
			this.name = name;
			this.target = target;
		}

		@Override
		public String getObjectName() {
			return this.name.toString();
		}

		@Override
		public Object getTarget() {
			return this.target;
		}

		@Override
		public Class<?> getFieldType(String field) {
			try {
				ResolvableType type = ValidationBindHandler.this.boundTypes.get(getName(field));
				Class<?> resolved = (type != null) ? type.resolve() : null;
				if (resolved != null) {
					return resolved;
				}
			}
			catch (Exception ex) {
			}
			return super.getFieldType(field);
		}

		@Override
		protected Object getActualFieldValue(String field) {
			try {
				return ValidationBindHandler.this.boundResults.get(getName(field));
			}
			catch (Exception ex) {
			}
			return null;
		}

		private ConfigurationPropertyName getName(String field) {
			return this.name.append(DataObjectPropertyName.toDashedForm(field));
		}

		ValidationErrors getValidationErrors() {
			Set<ConfigurationProperty> boundProperties = ValidationBindHandler.this.boundProperties.stream()
					.filter((property) -> this.name.isAncestorOf(property.getName()))
					.collect(Collectors.toCollection(LinkedHashSet::new));
			return new ValidationErrors(this.name, boundProperties, getAllErrors());
		}

	}

}
