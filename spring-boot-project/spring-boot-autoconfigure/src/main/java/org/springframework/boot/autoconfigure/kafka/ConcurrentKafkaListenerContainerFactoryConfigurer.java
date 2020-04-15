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

package org.springframework.boot.autoconfigure.kafka;

import java.time.Duration;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Listener;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AfterRollbackProcessor;
import org.springframework.kafka.listener.BatchErrorHandler;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.support.converter.MessageConverter;
import org.springframework.kafka.transaction.KafkaAwareTransactionManager;

/**
 * Configure {@link ConcurrentKafkaListenerContainerFactory} with sensible defaults.
 *
 * @author Gary Russell
 * @author Eddú Meléndez
 * @since 1.5.0
 */
public class ConcurrentKafkaListenerContainerFactoryConfigurer {

	private KafkaProperties properties;

	private MessageConverter messageConverter;

	private KafkaTemplate<Object, Object> replyTemplate;

	private KafkaAwareTransactionManager<Object, Object> transactionManager;

	private ConsumerAwareRebalanceListener rebalanceListener;

	private ErrorHandler errorHandler;

	private BatchErrorHandler batchErrorHandler;

	private AfterRollbackProcessor<Object, Object> afterRollbackProcessor;

	private RecordInterceptor<Object, Object> recordInterceptor;

	/**
	 * Set the {@link KafkaProperties} to use.
	 * @param properties the properties
	 */
	void setKafkaProperties(KafkaProperties properties) {
		this.properties = properties;
	}

	/**
	 * Set the {@link MessageConverter} to use.
	 * @param messageConverter the message converter
	 */
	void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the {@link KafkaTemplate} to use to send replies.
	 * @param replyTemplate the reply template
	 */
	void setReplyTemplate(KafkaTemplate<Object, Object> replyTemplate) {
		this.replyTemplate = replyTemplate;
	}

	/**
	 * Set the {@link KafkaAwareTransactionManager} to use.
	 * @param transactionManager the transaction manager
	 */
	void setTransactionManager(KafkaAwareTransactionManager<Object, Object> transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Set the {@link ConsumerAwareRebalanceListener} to use.
	 * @param rebalanceListener the rebalance listener.
	 * @since 2.2
	 */
	void setRebalanceListener(ConsumerAwareRebalanceListener rebalanceListener) {
		this.rebalanceListener = rebalanceListener;
	}

	/**
	 * Set the {@link ErrorHandler} to use.
	 * @param errorHandler the error handler
	 */
	void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Set the {@link BatchErrorHandler} to use.
	 * @param batchErrorHandler the error handler
	 */
	void setBatchErrorHandler(BatchErrorHandler batchErrorHandler) {
		this.batchErrorHandler = batchErrorHandler;
	}

	/**
	 * Set the {@link AfterRollbackProcessor} to use.
	 * @param afterRollbackProcessor the after rollback processor
	 */
	void setAfterRollbackProcessor(AfterRollbackProcessor<Object, Object> afterRollbackProcessor) {
		this.afterRollbackProcessor = afterRollbackProcessor;
	}

	/**
	 * Set the {@link RecordInterceptor} to use.
	 * @param recordInterceptor the record interceptor.
	 */
	void setRecordInterceptor(RecordInterceptor<Object, Object> recordInterceptor) {
		this.recordInterceptor = recordInterceptor;
	}

	/**
	 * Configure the specified Kafka listener container factory. The factory can be
	 * further tuned and default settings can be overridden.
	 * @param listenerFactory the {@link ConcurrentKafkaListenerContainerFactory} instance
	 * to configure
	 * @param consumerFactory the {@link ConsumerFactory} to use
	 */
	public void configure(ConcurrentKafkaListenerContainerFactory<Object, Object> listenerFactory,
			ConsumerFactory<Object, Object> consumerFactory) {
		listenerFactory.setConsumerFactory(consumerFactory);
		configureListenerFactory(listenerFactory);
		configureContainer(listenerFactory.getContainerProperties());
	}

	private void configureListenerFactory(ConcurrentKafkaListenerContainerFactory<Object, Object> factory) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Listener properties = this.properties.getListener();
		map.from(properties::getConcurrency).to(factory::setConcurrency);
		map.from(this.messageConverter).to(factory::setMessageConverter);
		map.from(this.replyTemplate).to(factory::setReplyTemplate);
		if (properties.getType().equals(Listener.Type.BATCH)) {
			factory.setBatchListener(true);
			factory.setBatchErrorHandler(this.batchErrorHandler);
		}
		else {
			factory.setErrorHandler(this.errorHandler);
		}
		map.from(this.afterRollbackProcessor).to(factory::setAfterRollbackProcessor);
		map.from(this.recordInterceptor).to(factory::setRecordInterceptor);
	}

	private void configureContainer(ContainerProperties container) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Listener properties = this.properties.getListener();
		map.from(properties::getAckMode).to(container::setAckMode);
		map.from(properties::getClientId).to(container::setClientId);
		map.from(properties::getAckCount).to(container::setAckCount);
		map.from(properties::getAckTime).as(Duration::toMillis).to(container::setAckTime);
		map.from(properties::getPollTimeout).as(Duration::toMillis).to(container::setPollTimeout);
		map.from(properties::getNoPollThreshold).to(container::setNoPollThreshold);
		map.from(properties::getIdleEventInterval).as(Duration::toMillis).to(container::setIdleEventInterval);
		map.from(properties::getMonitorInterval).as(Duration::getSeconds).as(Number::intValue)
				.to(container::setMonitorInterval);
		map.from(properties::getLogContainerConfig).to(container::setLogContainerConfig);
		map.from(properties::isMissingTopicsFatal).to(container::setMissingTopicsFatal);
		map.from(this.transactionManager).to(container::setTransactionManager);
		map.from(this.rebalanceListener).to(container::setConsumerRebalanceListener);
	}

}
