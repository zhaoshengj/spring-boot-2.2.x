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

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection based access to {@code com.sun.source.tree.ExpressionTree}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class ExpressionTree extends ReflectionWrapper {

	private final Class<?> literalTreeType = findClass("com.sun.source.tree.LiteralTree");

	private final Method literalValueMethod = findMethod(this.literalTreeType, "getValue");

	private final Class<?> methodInvocationTreeType = findClass("com.sun.source.tree.MethodInvocationTree");

	private final Method methodInvocationArgumentsMethod = findMethod(this.methodInvocationTreeType, "getArguments");

	private final Class<?> newArrayTreeType = findClass("com.sun.source.tree.NewArrayTree");

	private final Method arrayValueMethod = findMethod(this.newArrayTreeType, "getInitializers");

	ExpressionTree(Object instance) {
		super("com.sun.source.tree.ExpressionTree", instance);
	}

	String getKind() throws Exception {
		return findMethod("getKind").invoke(getInstance()).toString();
	}

	Object getLiteralValue() throws Exception {
		if (this.literalTreeType.isAssignableFrom(getInstance().getClass())) {
			return this.literalValueMethod.invoke(getInstance());
		}
		return null;
	}

	Object getFactoryValue() throws Exception {
		if (this.methodInvocationTreeType.isAssignableFrom(getInstance().getClass())) {
			List<?> arguments = (List<?>) this.methodInvocationArgumentsMethod.invoke(getInstance());
			if (arguments.size() == 1) {
				return new ExpressionTree(arguments.get(0)).getLiteralValue();
			}
		}
		return null;
	}

	List<? extends ExpressionTree> getArrayExpression() throws Exception {
		if (this.newArrayTreeType.isAssignableFrom(getInstance().getClass())) {
			List<?> elements = (List<?>) this.arrayValueMethod.invoke(getInstance());
			List<ExpressionTree> result = new ArrayList<>();
			if (elements == null) {
				return result;
			}
			for (Object element : elements) {
				result.add(new ExpressionTree(element));
			}
			return result;
		}
		return null;
	}

}
