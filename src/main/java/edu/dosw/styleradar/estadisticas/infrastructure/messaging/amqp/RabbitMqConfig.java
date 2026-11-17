package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ANALYTICS_EXCHANGE = "styleradar.domain.events";
    public static final String ANALYTICS_QUEUE = "statistics.analytics.events";
    public static final String ANALYTICS_ROUTING_KEY = "analytics.#";

    @Bean
    public TopicExchange analyticsExchange() {
        return new TopicExchange(ANALYTICS_EXCHANGE, true, false);
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE).build();
    }

    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, TopicExchange analyticsExchange) {
        return BindingBuilder.bind(analyticsQueue)
                .to(analyticsExchange)
                .with(ANALYTICS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonAmqpMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
