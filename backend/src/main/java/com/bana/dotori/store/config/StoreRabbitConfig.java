package com.bana.dotori.store.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 상품 가치 산정 큐(SHOP-03) 설정.
 *
 * <p>상품이 등록되면 이 익스체인지·라우팅 키로 메시지를 보내고, AI 서버가 이 큐를 구독해
 * value 를 산정한 뒤 채운다. 컨슈머(AI 서버)는 이 저장소 밖에 있다.
 */
@Configuration
public class StoreRabbitConfig {

	public static final String VALUE_EVALUATION_EXCHANGE = "store.product.exchange";
	public static final String VALUE_EVALUATION_QUEUE = "store.product.value-evaluation.queue";
	public static final String VALUE_EVALUATION_ROUTING_KEY = "store.product.value-evaluation";

	@Bean
	public DirectExchange storeProductExchange() {
		return new DirectExchange(VALUE_EVALUATION_EXCHANGE);
	}

	@Bean
	public Queue storeProductValueEvaluationQueue() {
		return new Queue(VALUE_EVALUATION_QUEUE, true);
	}

	@Bean
	public Binding storeProductValueEvaluationBinding(Queue storeProductValueEvaluationQueue,
			DirectExchange storeProductExchange) {
		return BindingBuilder.bind(storeProductValueEvaluationQueue)
			.to(storeProductExchange)
			.with(VALUE_EVALUATION_ROUTING_KEY);
	}

	@Bean
	public MessageConverter storeRabbitMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter storeRabbitMessageConverter) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(storeRabbitMessageConverter);
		return template;
	}
}
