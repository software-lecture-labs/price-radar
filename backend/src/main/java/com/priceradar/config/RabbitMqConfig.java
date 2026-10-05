package com.priceradar.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Base RabbitMQ topology: one topic exchange for application events, plus a
 * dead-letter exchange and queue so work queues added later have somewhere to
 * park a poison message instead of redelivering it forever.
 *
 * <p>Declare each work queue with {@link QueueBuilder#deadLetterExchange} set to
 * {@link MessagingProperties#deadLetterExchange()}.
 */
@Configuration
public class RabbitMqConfig {

	@Bean
	TopicExchange eventsExchange(MessagingProperties properties) {
		return new TopicExchange(properties.exchange(), true, false);
	}

	@Bean
	TopicExchange deadLetterExchange(MessagingProperties properties) {
		return new TopicExchange(properties.deadLetterExchange(), true, false);
	}

	@Bean
	Queue deadLetterQueue(MessagingProperties properties) {
		return QueueBuilder.durable(properties.deadLetterQueue()).build();
	}

	@Bean
	Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
		return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("#");
	}

	@Bean
	MessageConverter jsonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter,
			MessagingProperties properties) {
		var template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(messageConverter);
		template.setExchange(properties.exchange());
		return template;
	}

}
