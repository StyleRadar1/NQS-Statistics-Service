package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Configuration
public class RabbitMqConfig {

    public static final String ANALYTICS_EXCHANGE = "styleradar.exchange";
    public static final String DOMAIN_EVENTS_QUEUE = "styleradar.estadisticas.eventos-dominio.q";
    public static final String DEAD_LETTER_QUEUE = "styleradar.estadisticas.dlq";

    @Bean
    public TopicExchange analyticsExchange() {
        return new TopicExchange(ANALYTICS_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Queue domainEventsQueue() {
        return QueueBuilder.durable(DOMAIN_EVENTS_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Declarables domainEventBindings(Queue domainEventsQueue, TopicExchange analyticsExchange) {
        return new Declarables(
                BindingBuilder.bind(domainEventsQueue).to(analyticsExchange)
                        .with(DomainEventMapper.USER_REGISTERED_ROUTING_KEY),
                BindingBuilder.bind(domainEventsQueue).to(analyticsExchange)
                        .with(DomainEventMapper.INVENTORY_UPDATED_ROUTING_KEY),
                BindingBuilder.bind(domainEventsQueue).to(analyticsExchange)
                        .with(DomainEventMapper.GARMENT_PUBLISHED_ROUTING_KEY),
                BindingBuilder.bind(domainEventsQueue).to(analyticsExchange)
                        .with(DomainEventMapper.PLAYLIST_CREATED_ROUTING_KEY),
                BindingBuilder.bind(domainEventsQueue).to(analyticsExchange)
                        .with(DomainEventMapper.SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY),
                BindingBuilder.bind(domainEventsQueue).to(analyticsExchange)
                        .with(DomainEventMapper.SUBSCRIPTION_PLAN_UPDATED_ROUTING_KEY),
                BindingBuilder.bind(domainEventsQueue).to(analyticsExchange)
                        .with(DomainEventMapper.DONATION_COMPLETED_ROUTING_KEY)
        );
    }

    @Bean
    public MessageConverter jacksonAmqpMessageConverter(ObjectMapper objectMapper) {
        ObjectMapper amqpMapper = objectMapper.copy();
        amqpMapper.registerModule(new JavaTimeModule());
        amqpMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(amqpMapper);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RetryOperationsInterceptor rabbitRetryInterceptor() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000L);

        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
                MessageConversionException.class, false,
                AmqpRejectAndDontRequeueException.class, false,
                IllegalArgumentException.class, false
        );
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(4, retryableExceptions, true, true);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);

        return RetryInterceptorBuilder.stateless()
                .retryOperations(retryTemplate)
                .recoverer((args, cause) -> {
                    throw new AmqpRejectAndDontRequeueException(
                            "Message rejected and routed to DLQ", cause);
                })
                .build();
    }

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jacksonAmqpMessageConverter,
            RetryOperationsInterceptor rabbitRetryInterceptor,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonAmqpMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(rabbitRetryInterceptor);
        factory.setAutoStartup(autoStartup);
        return factory;
    }
}
