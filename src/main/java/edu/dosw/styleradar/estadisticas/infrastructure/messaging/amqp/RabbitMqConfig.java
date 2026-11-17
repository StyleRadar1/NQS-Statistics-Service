package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ANALYTICS_EXCHANGE = "styleradar.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "styleradar.dlx";
    public static final String ANALYTICS_QUEUE = "statistics.analytics.events";
    public static final String ANALYTICS_DLQ = "statistics.analytics.events.dlq";
    public static final String ANALYTICS_ROUTING_KEY = "analytics.#";

    @Bean
    public TopicExchange analyticsExchange() {
        return new TopicExchange(ANALYTICS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ANALYTICS_QUEUE)
                .build();
    }

    @Bean
    public Queue analyticsDeadLetterQueue() {
        return QueueBuilder.durable(ANALYTICS_DLQ).build();
    }

    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, TopicExchange analyticsExchange) {
        return BindingBuilder.bind(analyticsQueue)
                .to(analyticsExchange)
                .with(ANALYTICS_ROUTING_KEY);
    }

    @Bean
    public Binding analyticsDeadLetterBinding(Queue analyticsDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(analyticsDeadLetterQueue)
                .to(deadLetterExchange)
                .with(ANALYTICS_QUEUE);
    }

    @Bean
    public MessageConverter jacksonAmqpMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
