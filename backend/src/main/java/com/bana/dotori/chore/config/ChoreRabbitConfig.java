package com.bana.dotori.chore.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 할 일 보상 산정 큐(CHORE-04) 설정.
 *
 * <p>할 일이 등록되면 이 익스체인지·라우팅 키로 메시지를 보내고, AI 서버가 이 큐를 구독해
 * value 와 reward_points 를 채운다. 컨슈머(AI 서버)는 이 저장소 밖에 있다.
 *
 * <p>RabbitTemplate 과 MessageConverter 는 여기서 만들지 않는다.
 * StoreRabbitConfig 가 이미 등록해 두었고, 같은 이름의 빈을 또 만들면 기동이 실패한다.
 */
@Configuration
public class ChoreRabbitConfig {

	public static final String REWARD_EVALUATION_EXCHANGE = "chore.exchange";
	public static final String REWARD_EVALUATION_QUEUE = "chore.reward-evaluation.queue";
	public static final String REWARD_EVALUATION_ROUTING_KEY = "chore.reward-evaluation";

	@Bean
	public DirectExchange choreExchange() {
		return new DirectExchange(REWARD_EVALUATION_EXCHANGE);
	}

	@Bean
	public Queue choreRewardEvaluationQueue() {
		return new Queue(REWARD_EVALUATION_QUEUE, true);
	}

	@Bean
	public Binding choreRewardEvaluationBinding(Queue choreRewardEvaluationQueue, DirectExchange choreExchange) {
		return BindingBuilder.bind(choreRewardEvaluationQueue)
			.to(choreExchange)
			.with(REWARD_EVALUATION_ROUTING_KEY);
	}
}
