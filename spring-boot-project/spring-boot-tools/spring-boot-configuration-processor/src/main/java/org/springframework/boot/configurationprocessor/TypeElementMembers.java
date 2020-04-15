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

package org.springframework.boot.configurationprocessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

/**
 * Provides access to relevant {@link TypeElement} members.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class TypeElementMembers {

	private static final String OBJECT_CLASS_NAME = Object.class.getName();

	private final MetadataGenerationEnvironment env;

	private final TypeElement targetType;

	private final Map<String, VariableElement> fields = new LinkedHashMap<>();

	private final Map<String, ExecutableElement> publicGetters = new LinkedHashMap<>();

	private final Map<String, List<ExecutableElement>> publicSetters = new LinkedHashMap<>();

	TypeElementMembers(MetadataGenerationEnvironment env, TypeElement targetType) {
		this.env = env;
		this.targetType = targetType;
		process(targetType);
	}

	private void process(TypeElement element) {
		for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
			processMethod(method);
		}
		for (VariableElement field : ElementFilter.fieldsIn(element.getEnclosedElements())) {
			processField(field);
		}
		Element superType = this.env.getTypeUtils().asElement(element.getSuperclass());
		if (superType instanceof TypeElement && !OBJECT_CLASS_NAME.equals(superType.toString())) {
			process((TypeElement) superType);
		}
	}

	private void processMethod(ExecutableElement method) {
		if (isPublic(method)) {
			String name = method.getSimpleName().toString();
			if (isGetter(method) && !this.publicGetters.containsKey(name)) {
				this.publicGetters.put(getAccessorName(name), method);
			}
			else if (isSetter(method)) {
				String propertyName = getAccessorName(name);
				List<ExecutableElement> matchingSetters = this.publicSetters.computeIfAbsent(propertyName,
						(k) -> new ArrayList<>());
				TypeMirror paramType = method.getParameters().get(0).asType();
				if (getMatchingSetter(matchingSetters, paramType) == null) {
					matchingSetters.add(method);
				}
			}
		}
	}

	private boolean isPublic(ExecutableElement method) {
		Set<Modifier> modifiers = method.getModifiers();
		return modifiers.contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.ABSTRACT)
				&& !modifiers.contains(Modifier.STATIC);
	}

	private ExecutableElement getMatchingSetter(List<ExecutableElement> candidates, TypeMirror type) {
		for (ExecutableElement candidate : candidates) {
			TypeMirror paramType = candidate.getParameters().get(0).asType();
			if (this.env.getTypeUtils().isSameType(paramType, type)) {
				return candidate;
			}
		}
		return null;
	}

	private boolean isGetter(ExecutableElement method) {
		String name = method.getSimpleName().toString();
		return ((name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2))
				&& method.getParameters().isEmpty() && (TypeKind.VOID != method.getReturnType().getKind());
	}

	private boolean isSetter(ExecutableElement method) {
		final String name = method.getSimpleName().toString();
		return (name.startsWith("set") && name.length() > 3 && method.getParameters().size() == 1
				&& isSetterReturnType(method));
	}

	private boolean isSetterReturnType(ExecutableElement method) {
		TypeMirror returnType = method.getReturnType();
		if (TypeKind.VOID == returnType.getKind()) {
			return true;
		}
		if (TypeKind.DECLARED == returnType.getKind()
				&& this.env.getTypeUtils().isSameType(method.getEnclosingElement().asType(), returnType)) {
			return true;
		}
		if (TypeKind.TYPEVAR == returnType.getKind()) {
			String resolvedType = this.env.getTypeUtils().getType(this.targetType, returnType);
			return (resolvedType != null
					&& resolvedType.equals(this.env.getTypeUtils().getQualifiedName(this.targetType)));
		}
		return false;
	}

	private String getAccessorName(String methodName) {
		String name = methodName.startsWith("is") ? methodName.substring(2) : methodName.substring(3);
		name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
		return name;
	}

	private void processField(VariableElement field) {
		String name = field.getSimpleName().toString();
		if (!this.fields.containsKey(name)) {
			this.fields.put(name, field);
		}
	}

	Map<String, VariableElement> getFields() {
		return Collections.unmodifiableMap(this.fields);
	}

	Map<String, ExecutableElement> getPublicGetters() {
		return Collections.unmodifiableMap(this.publicGetters);
	}

	ExecutableElement getPublicGetter(String name, TypeMirror type) {
		ExecutableElement candidate = this.publicGetters.get(name);
		if (candidate != null) {
			TypeMirror returnType = candidate.getReturnType();
			if (this.env.getTypeUtils().isSameType(returnType, type)) {
				return candidate;
			}
			TypeMirror alternative = this.env.getTypeUtils().getWrapperOrPrimitiveFor(type);
			if (alternative != null && this.env.getTypeUtils().isSameType(returnType, alternative)) {
				return candidate;
			}
		}
		return null;
	}

	ExecutableElement getPublicSetter(String name, TypeMirror type) {
		List<ExecutableElement> candidates = this.publicSetters.get(name);
		if (candidates != null) {
			ExecutableElement matching = getMatchingSetter(candidates, type);
			if (matching != null) {
				return matching;
			}
			TypeMirror alternative = this.env.getTypeUtils().getWrapperOrPrimitiveFor(type);
			if (alternative != null) {
				return getMatchingSetter(candidates, alternative);
			}
		}
		return null;
	}

}
