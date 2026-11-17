package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Configuration
public class RabbitMqConfig {

    public static final String ANALYTICS_EXCHANGE = "styleradar.exchange";
    public static final String ALERTA_CREADA_QUEUE = "styleradar.estadisticas.alerta-creada.q";
    public static final String DEAD_LETTER_QUEUE = "styleradar.estadisticas.dlq";
    public static final String ALERTA_CREADA_ROUTING_KEY = "styleradar.notificaciones.alerta.creada";

    @Bean
    public TopicExchange analyticsExchange() {
        return new TopicExchange(ANALYTICS_EXCHANGE, true, false);
    }

    /**
     * Se utiliza el default exchange ("") con x-dead-letter-routing-key apuntando directamente
     * a styleradar.estadisticas.dlq en lugar de declarar styleradar.dlx.
     * Razón: styleradar.dlx no está confirmado como existente ni con permisos garantizados para
     * el usuario de ms-estadisticas en el broker; usar el default exchange evita fallos de
     * declaración/permisos (PRECONDITION_FAILED o ACCESS_REFUSED) al iniciar el servicio.
     */
    @Bean
    public Queue alertaCreadaQueue() {
        return QueueBuilder.durable(ALERTA_CREADA_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding alertaCreadaBinding(Queue alertaCreadaQueue, TopicExchange analyticsExchange) {
        return BindingBuilder.bind(alertaCreadaQueue)
                .to(analyticsExchange)
                .with(ALERTA_CREADA_ROUTING_KEY);
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
            RetryOperationsInterceptor rabbitRetryInterceptor
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonAmqpMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(rabbitRetryInterceptor);
        return factory;
    }
}
