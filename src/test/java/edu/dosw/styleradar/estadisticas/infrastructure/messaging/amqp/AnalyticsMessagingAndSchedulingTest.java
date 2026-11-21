package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.estadisticas.domain.model.AnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.ConsolidateCountersUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.in.RegisterAnalyticsEventUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.ProcessedEventEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.ProcessedEventJpaRepository;
import edu.dosw.styleradar.estadisticas.infrastructure.scheduling.CounterConsolidationScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsMessagingAndSchedulingTest {

    private final EventToMetricMapper mapper = new EventToMetricMapper();

    @Test
    void mapsValidAlertaCreadaPayloadToPlatformMetric() {
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 11, 2, 12, 30, 0);
        AlertaCreadaEventPayload payload = new AlertaCreadaEventPayload(
                "evt-alr-1001",
                75L,
                501L,
                "ITEM_AGOTADO",
                "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                null,
                "IN_APP",
                fechaCreacion);

        AnalyticsEvent event = mapper.toAnalyticsEvent(payload);

        assertEquals("evt-alr-1001", event.eventId());
        assertEquals(MetricCode.PLATFORM_ALERTS_CREATED, event.metricCode());
        assertEquals(ScopeType.PLATFORM, event.scope().type());
        assertEquals("platform", event.scope().id());
        assertEquals(BigDecimal.ONE, event.value());
        assertEquals(fechaCreacion.toInstant(ZoneOffset.UTC), event.occurredAt());
        assertNull(event.paymentStatus());
    }

    @Test
    void rejectsNullOrIncompleteAlertaCreadaPayloads() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 2, 12, 30, 0);
        assertThrows(NullPointerException.class, () -> mapper.toAnalyticsEvent(null));

        assertThrows(IllegalArgumentException.class, () -> mapper.toAnalyticsEvent(
                new AlertaCreadaEventPayload(" ", 75L, 501L, "ITEM_AGOTADO", "1045", null, "IN_APP", now)));
        assertThrows(NullPointerException.class, () -> mapper.toAnalyticsEvent(
                new AlertaCreadaEventPayload("evt-1", null, 501L, "ITEM_AGOTADO", "1045", null, "IN_APP", now)));
        assertThrows(NullPointerException.class, () -> mapper.toAnalyticsEvent(
                new AlertaCreadaEventPayload("evt-1", 75L, null, "ITEM_AGOTADO", "1045", null, "IN_APP", now)));
        assertThrows(IllegalArgumentException.class, () -> mapper.toAnalyticsEvent(
                new AlertaCreadaEventPayload("evt-1", 75L, 501L, "", "1045", null, "IN_APP", now)));
        assertThrows(NullPointerException.class, () -> mapper.toAnalyticsEvent(
                new AlertaCreadaEventPayload("evt-1", 75L, 501L, "ITEM_AGOTADO", "1045", null, "IN_APP", null)));
    }

    @Test
    void listenerDeduplicatesByEventoIdAndDiscardsDuplicatesWithoutError() {
        RegisterAnalyticsEventUseCase registerUseCase = mock(RegisterAnalyticsEventUseCase.class);
        ProcessedEventJpaRepository processedRepo = mock(ProcessedEventJpaRepository.class);
        AnalyticsEventListener listener = new AnalyticsEventListener(mapper, registerUseCase, processedRepo);

        AlertaCreadaEventPayload payload = new AlertaCreadaEventPayload(
                "evt-alr-1001",
                75L,
                501L,
                "ITEM_AGOTADO",
                "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                null,
                "IN_APP",
                LocalDateTime.of(2026, 11, 2, 12, 30, 0));

        when(processedRepo.existsById("evt-alr-1001")).thenReturn(false, true);
        listener.receive(payload);
        listener.receive(payload);

        verify(processedRepo, times(1)).saveAndFlush(any(ProcessedEventEntity.class));
        verify(registerUseCase, times(1)).register(mapper.toAnalyticsEvent(payload));

        when(processedRepo.existsById("evt-alr-concurrent")).thenReturn(false);
        when(processedRepo.saveAndFlush(any(ProcessedEventEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        AlertaCreadaEventPayload concurrentPayload = new AlertaCreadaEventPayload(
                "evt-alr-concurrent", 76L, 502L, "ITEM_AGOTADO", "1046", null, "IN_APP",
                LocalDateTime.of(2026, 11, 2, 12, 31, 0));
        listener.receive(concurrentPayload);

        verify(registerUseCase, never()).register(mapper.toAnalyticsEvent(concurrentPayload));
    }

    @Test
    void listenerRejectsInvalidPayloadDirectlyToDlq() {
        RegisterAnalyticsEventUseCase registerUseCase = mock(RegisterAnalyticsEventUseCase.class);
        ProcessedEventJpaRepository processedRepo = mock(ProcessedEventJpaRepository.class);
        AnalyticsEventListener listener = new AnalyticsEventListener(mapper, registerUseCase, processedRepo);

        AlertaCreadaEventPayload invalidPayload = new AlertaCreadaEventPayload(
                "", 75L, 501L, "ITEM_AGOTADO", "1045", null, "IN_APP",
                LocalDateTime.of(2026, 11, 2, 12, 30, 0));

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.receive(invalidPayload));
        verify(registerUseCase, never()).register(any());
    }

    @Test
    void deserializesJsonWithoutTypeIdHeaderAndRejectsInvalidJsonDirectlyToDlq() throws Exception {
        RabbitMqConfig config = new RabbitMqConfig();
        MessageConverter converter = config.jacksonAmqpMessageConverter(new ObjectMapper());

        String validJson = """
                {
                  "eventoId": "evt-alr-1001",
                  "alertaId": 75,
                  "usuarioId": 501,
                  "tipoAlerta": "ITEM_AGOTADO",
                  "itemId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "busquedaGuardadaId": null,
                  "canal": "IN_APP",
                  "fechaCreacion": "2026-11-02T12:30:00"
                }
                """;
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Method receiveMethod = AnalyticsEventListener.class.getMethod("receive", AlertaCreadaEventPayload.class);
        props.setInferredArgumentType(new MethodParameter(receiveMethod, 0).getGenericParameterType());

        Object converted = converter.fromMessage(new Message(validJson.getBytes(StandardCharsets.UTF_8), props));
        AlertaCreadaEventPayload payload = assertInstanceOf(AlertaCreadaEventPayload.class, converted);
        assertEquals("evt-alr-1001", payload.eventoId());
        assertEquals(75L, payload.alertaId());
        assertEquals(LocalDateTime.of(2026, 11, 2, 12, 30, 0), payload.fechaCreacion());

        Message invalidMessage = new Message("{not-valid-json".getBytes(StandardCharsets.UTF_8), props);
        assertThrows(MessageConversionException.class, () -> converter.fromMessage(invalidMessage));

        RetryOperationsInterceptor retryInterceptor = config.rabbitRetryInterceptor();
        AtomicInteger attempts = new AtomicInteger(0);
        ChannelAwareMessageListener failingListener = (msg, channel) -> {
            attempts.incrementAndGet();
            throw new ListenerExecutionFailedException(
                    "Conversion failed",
                    new MessageConversionException("Invalid JSON"),
                    msg);
        };
        ProxyFactory proxyFactory = new ProxyFactory(failingListener);
        proxyFactory.addAdvice(retryInterceptor);
        ChannelAwareMessageListener proxiedListener = (ChannelAwareMessageListener) proxyFactory.getProxy();

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> proxiedListener.onMessage(invalidMessage, null));
        assertEquals(1, attempts.get());
    }

    @Test
    void schedulerInvokesConsolidationUseCase() {
        ConsolidateCountersUseCase consolidateUseCase = mock(ConsolidateCountersUseCase.class);
        CounterConsolidationScheduler scheduler = new CounterConsolidationScheduler(consolidateUseCase);

        scheduler.consolidateCounters();

        verify(consolidateUseCase).consolidate();
    }

    @Test
    void configuresOfficialQueueDlqAndTopicExchangeBinding() {
        RabbitMqConfig config = new RabbitMqConfig();
        TopicExchange exchange = config.analyticsExchange();
        Queue queue = config.alertaCreadaQueue();
        Queue dlq = config.deadLetterQueue();
        Binding binding = config.alertaCreadaBinding(queue, exchange);

        assertEquals("styleradar.exchange", exchange.getName());
        assertTrue(exchange.isDurable());
        assertEquals("styleradar.estadisticas.alerta-creada.q", queue.getName());
        assertTrue(queue.isDurable());
        assertEquals("", queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("styleradar.estadisticas.dlq", queue.getArguments().get("x-dead-letter-routing-key"));
        assertEquals("styleradar.estadisticas.dlq", dlq.getName());
        assertTrue(dlq.isDurable());
        assertEquals("styleradar.notificaciones.alerta.creada", binding.getRoutingKey());
        assertEquals("styleradar.exchange", binding.getExchange());
        assertEquals("styleradar.estadisticas.alerta-creada.q", binding.getDestination());
    }
}
