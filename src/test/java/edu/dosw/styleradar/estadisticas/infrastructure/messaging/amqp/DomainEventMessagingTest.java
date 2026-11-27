package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.DomainAnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.DomainEventType;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import edu.dosw.styleradar.estadisticas.domain.port.in.ProcessDomainEventUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.ProcessedEventEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.ProcessedEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
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
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DomainEventMessagingTest {

    private final DomainEventMapper mapper = new DomainEventMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsInventoryEnvelopeForCreationUpdateAndEstadoField() throws Exception {
        String updatedJson = """
                {
                  "eventId": "9c2e8a74-1f3b-4c5d-8e9a-0b1c2d3e4f5a",
                  "eventType": "styleradar.inventario.actualizado",
                  "timestamp": "2026-11-24T23:00:00Z",
                  "data": {
                    "tiendaId": "store-17",
                    "sedeId": "fc942335-ab30-350a-b6a5-577cf0ca2fb5",
                    "stockAnterior": 10,
                    "stockDisponible": 3,
                    "estado": "ACTUALIZADO"
                  }
                }
                """;
        RabbitMqConfig config = new RabbitMqConfig();
        MessageConverter converter = config.jacksonAmqpMessageConverter(objectMapper);
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setInferredArgumentType(new MethodParameter(
                DomainEventListener.class.getMethod("receive", DomainEventPayload.class, String.class),
                0).getGenericParameterType());
        DomainEventPayload updatedPayload = (DomainEventPayload) converter.fromMessage(
                new Message(updatedJson.getBytes(StandardCharsets.UTF_8), properties));
        DomainAnalyticsEvent updatedEvent = mapper.toDomainEvent(
                updatedPayload, DomainEventMapper.INVENTORY_UPDATED_ROUTING_KEY);

        assertEquals(DomainEventType.INVENTORY_STATE_CHANGED, updatedEvent.eventType());
        assertEquals("store-17", updatedEvent.scopeId());
        assertEquals(CategoryCode.AVAILABLE, updatedEvent.previousCategoryCode());
        assertEquals(CategoryCode.LOW_STOCK, updatedEvent.categoryCode());
        assertNull(updatedEvent.paymentStatus());

        DomainEventPayload initialPayload = objectMapper.readValue("""
                {
                  "eventId": "inv-create-1",
                  "timestamp": "2026-11-24T23:00:00Z",
                  "data": {
                    "sedeId": "sede-1",
                    "stockAnterior": null,
                    "stockDisponible": 0
                  }
                }
                """, DomainEventPayload.class);
        DomainAnalyticsEvent initialEvent = mapper.toDomainEvent(
                initialPayload, DomainEventMapper.INVENTORY_UPDATED_ROUTING_KEY);
        assertEquals("sede-1", initialEvent.scopeId());
        assertNull(initialEvent.previousCategoryCode());
        assertEquals(CategoryCode.OUT_OF_STOCK, initialEvent.categoryCode());
    }

    @Test
    void mapsAllSevenSupportedProducerContracts() throws Exception {
        DomainEventPayload userBuyerPayload = objectMapper.readValue("""
                {
                  "eventId": "usr-1",
                  "occurredAt": "2026-11-24T16:30:00Z",
                  "data": { "rol": "BUYER", "estado": "ACTIVO" }
                }
                """, DomainEventPayload.class);
        DomainAnalyticsEvent userBuyer = mapper.toDomainEvent(
                userBuyerPayload, DomainEventMapper.USER_REGISTERED_ROUTING_KEY);
        assertEquals(DomainEventType.USER_REGISTERED, userBuyer.eventType());
        assertEquals(CategoryCode.PLAN_RADAR, userBuyer.categoryCode());

        DomainEventPayload userStorePayload = objectMapper.readValue("""
                {
                  "eventId": "usr-2",
                  "occurredAt": "2026-11-24T16:30:00Z",
                  "data": { "rol": "STORE_ADMIN" }
                }
                """, DomainEventPayload.class);
        assertNull(mapper.toDomainEvent(userStorePayload, DomainEventMapper.USER_REGISTERED_ROUTING_KEY).categoryCode());

        DomainEventPayload garmentPayload = objectMapper.readValue("""
                {
                  "eventoId": "cat-1",
                  "fechaEvento": "2026-11-24T15:00:00Z",
                  "data": { "itemId": "garment-101", "tiendaId": "store-17", "precio": 89000 }
                }
                """, DomainEventPayload.class);
        DomainAnalyticsEvent garmentEvent = mapper.toDomainEvent(
                garmentPayload, DomainEventMapper.GARMENT_PUBLISHED_ROUTING_KEY);
        assertEquals(DomainEventType.GARMENT_PUBLISHED, garmentEvent.eventType());
        assertNull(garmentEvent.amount());

        DomainEventPayload playlistPayload = objectMapper.readValue("""
                {
                  "eventId": "pl-1",
                  "occurredAt": "2026-11-24T16:15:00Z"
                }
                """, DomainEventPayload.class);
        assertEquals(DomainEventType.PLAYLIST_CREATED,
                mapper.toDomainEvent(playlistPayload, DomainEventMapper.PLAYLIST_CREATED_ROUTING_KEY).eventType());

        DomainEventPayload paymentPayload = objectMapper.readValue("""
                {
                  "eventId": "pay-1",
                  "eventType": "PagoConfirmadoEvent",
                  "timestamp": "2026-11-24T15:00:00Z",
                  "data": { "estado": "APPROVED", "monto": 14900, "moneda": "COP" }
                }
                """, DomainEventPayload.class);
        DomainAnalyticsEvent payment = mapper.toDomainEvent(
                paymentPayload, DomainEventMapper.SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY);
        assertEquals(DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED, payment.eventType());
        assertEquals(PaymentStatus.APROBADO, payment.paymentStatus());
        assertEquals(new BigDecimal("14900"), payment.amount());

        DomainEventPayload planPayload = objectMapper.readValue("""
                {
                  "eventId": "plan-1",
                  "eventType": "SuscripcionPlanActualizado",
                  "timestamp": "2026-11-24T15:00:05Z",
                  "data": {
                    "tipoSuscriptor": "USUARIO",
                    "estado": "ACTIVA",
                    "planAnterior": "PLAN_RADAR",
                    "planActual": "RADAR_PLUS"
                  }
                }
                """, DomainEventPayload.class);
        DomainAnalyticsEvent plan = mapper.toDomainEvent(
                planPayload, DomainEventMapper.SUBSCRIPTION_PLAN_UPDATED_ROUTING_KEY);
        assertEquals(DomainEventType.SUBSCRIPTION_PLAN_CHANGED, plan.eventType());
        assertEquals(CategoryCode.PLAN_RADAR, plan.previousCategoryCode());
        assertEquals(CategoryCode.PLAN_RADAR_PLUS, plan.categoryCode());
        assertEquals("USUARIO", plan.subscriberType());

        DomainEventPayload donationPayload = objectMapper.readValue("""
                {
                  "eventId": "don-1",
                  "eventType": "DonationDeliveredEvent",
                  "occurredAt": "2026-11-24T15:00:00Z",
                  "data": { "foundationId": 501, "cantidad": 4 }
                }
                """, DomainEventPayload.class);
        DomainAnalyticsEvent donation = mapper.toDomainEvent(
                donationPayload, DomainEventMapper.DONATION_COMPLETED_ROUTING_KEY);
        assertEquals(DomainEventType.DONATION_COMPLETED, donation.eventType());
        assertEquals("501", donation.scopeId());
        assertEquals(BigDecimal.valueOf(4), donation.amount());
    }

    @Test
    void mapsPagosSuscripcionesEdgeCasesAndTimestampFormats() throws Exception {
        DomainEventPayload confirmedStatusPayment = objectMapper.readValue("""
                {
                  "eventId": "pay-confirmed",
                  "timestamp": [2026, 11, 24, 15, 0, 5],
                  "data": {
                    "ordenId": 21,
                    "suscripcionId": 15,
                    "tipoSuscriptor": "USUARIO",
                    "planId": 2,
                    "estado": "CONFIRMED",
                    "valor": 29900
                  }
                }
                """, DomainEventPayload.class);
        DomainAnalyticsEvent confirmedPayment = mapper.toDomainEvent(
                confirmedStatusPayment, DomainEventMapper.SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY);
        assertEquals(PaymentStatus.APROBADO, confirmedPayment.paymentStatus());
        assertEquals(new BigDecimal("29900"), confirmedPayment.amount());
        assertEquals(Instant.parse("2026-11-24T15:00:05Z"), confirmedPayment.occurredAt());

        DomainEventPayload flatRejectedPayment = objectMapper.readValue("""
                {
                  "eventId": "pay-flat-rejected",
                  "timestamp": 1763996400,
                  "estado": "REJECTED",
                  "monto": 14900
                }
                """, DomainEventPayload.class);
        DomainAnalyticsEvent rejectedPayment = mapper.toDomainEvent(
                flatRejectedPayment, DomainEventMapper.SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY);
        assertEquals(PaymentStatus.RECHAZADO, rejectedPayment.paymentStatus());
        assertEquals(Instant.ofEpochSecond(1763996400L), rejectedPayment.occurredAt());

        DomainEventPayload suspendedStoreByState = objectMapper.readValue("""
                {
                  "eventId": "plan-store-susp",
                  "timestamp": "2026-11-24 15:00:00",
                  "data": {
                    "tipoSuscriptor": "ALMACEN",
                    "estado": "SUSPENDIDA",
                    "planAnterior": "STORE",
                    "planActual": null
                  }
                }
                """, DomainEventPayload.class);
        DomainAnalyticsEvent storeSuspension = mapper.toDomainEvent(
                suspendedStoreByState, DomainEventMapper.SUBSCRIPTION_PLAN_UPDATED_ROUTING_KEY);
        assertEquals(CategoryCode.PLAN_STORE_MONTHLY, storeSuspension.previousCategoryCode());
        assertEquals(CategoryCode.SUSPENDED, storeSuspension.categoryCode());
        assertEquals("ALMACEN", storeSuspension.subscriberType());

        DomainEventPayload planByNumericId = objectMapper.readValue("""
                {
                  "eventId": "plan-id-activation",
                  "timestamp": "2026-11-24",
                  "data": {
                    "tipoSuscriptor": "BUYER",
                    "planAnteriorId": 1,
                    "planId": 2
                  }
                }
                """, DomainEventPayload.class);
        DomainAnalyticsEvent numericPlan = mapper.toDomainEvent(
                planByNumericId, DomainEventMapper.SUBSCRIPTION_PLAN_UPDATED_ROUTING_KEY);
        assertEquals(CategoryCode.PLAN_RADAR, numericPlan.previousCategoryCode());
        assertEquals(CategoryCode.PLAN_RADAR_PLUS, numericPlan.categoryCode());
    }

    @Test
    void rejectsInvalidPayloadsAndUnknownRoutingKeys() {
        assertTrue(mapper.isSupportedRoutingKey(DomainEventMapper.SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY));
        assertFalse(mapper.isSupportedRoutingKey("styleradar.pagos.suscripcion.suspendida"));

        assertThrows(NullPointerException.class, () -> mapper.toDomainEvent(null, DomainEventMapper.PLAYLIST_CREATED_ROUTING_KEY));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toDomainEvent(payload(" ", "PLAYLIST_CREATED"), null));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toDomainEvent(payload("e1", "UNKNOWN_TYPE"), null));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toDomainEvent(payload("e1", null), "styleradar.desconocido.evento"));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toDomainEvent(payload("e1", null), null));
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomainEvent(
                new DomainEventPayload("e3", "styleradar.pagos.suscripcion.plan.actualizado", "2026-11-24T23:00:00Z",
                        null, null, null, null, null, null,
                        new DomainEventPayload.DomainEventData(
                                null, null, null, null, null, null, null, null,
                                null, null, null, null, null, "BUYER", null, "PLAN_INEXISTENTE")),
                DomainEventMapper.SUBSCRIPTION_PLAN_UPDATED_ROUTING_KEY));
    }

    @Test
    void listenerDeduplicatesIgnoresOutOfScopeRoutingKeysAndRoutesInvalidEventsToDlq() throws Exception {
        ProcessDomainEventUseCase useCase = mock(ProcessDomainEventUseCase.class);
        ProcessedEventJpaRepository processedRepo = mock(ProcessedEventJpaRepository.class);
        DomainEventListener listener = new DomainEventListener(
                mapper, useCase, processedRepo, TransactionOperations.withoutTransaction());

        listener.receive(payload("evt-legacy-susp", "styleradar.pagos.suscripcion.suspendida"),
                "styleradar.pagos.suscripcion.suspendida");
        verifyNoInteractions(processedRepo, useCase);

        DomainEventPayload validPayload = payload("evt-dom-1", "PLAYLIST_CREATED");
        when(processedRepo.existsById("evt-dom-1")).thenReturn(false, true);

        listener.receive(validPayload, DomainEventMapper.PLAYLIST_CREATED_ROUTING_KEY);
        listener.receive(validPayload, DomainEventMapper.PLAYLIST_CREATED_ROUTING_KEY);

        verify(processedRepo, times(1)).saveAndFlush(any(ProcessedEventEntity.class));
        verify(useCase, times(1)).process(any(DomainAnalyticsEvent.class));

        when(processedRepo.existsById("evt-dom-conc")).thenReturn(false);
        when(processedRepo.saveAndFlush(any(ProcessedEventEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        listener.receive(payload("evt-dom-conc", "PLAYLIST_CREATED"), null);
        verify(useCase, times(1)).process(any(DomainAnalyticsEvent.class));

        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.receive(payload(" ", "PLAYLIST_CREATED"), null));

        when(processedRepo.existsById("evt-dom-bad-usecase")).thenReturn(false);
        when(processedRepo.saveAndFlush(any(ProcessedEventEntity.class)))
                .thenReturn(new ProcessedEventEntity("evt-dom-bad-usecase", "DONATION_COMPLETED", Instant.now()));
        doThrow(new IllegalArgumentException("scopeId required")).when(useCase).process(any());
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.receive(payload("evt-dom-bad-usecase", "DONATION_COMPLETED"), null));

        RabbitMqConfig config = new RabbitMqConfig();
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
        Message invalidMessage = new Message("{bad".getBytes(StandardCharsets.UTF_8), new MessageProperties());
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> proxiedListener.onMessage(invalidMessage, null));
        assertEquals(1, attempts.get());
    }

    @Test
    void configuresExactBindings() {
        RabbitMqConfig config = new RabbitMqConfig();
        TopicExchange exchange = config.analyticsExchange();
        Queue domainQueue = config.domainEventsQueue();
        Queue dlq = config.deadLetterQueue();
        Declarables bindings = config.domainEventBindings(domainQueue, exchange);

        assertEquals("styleradar.estadisticas.eventos-dominio.q", domainQueue.getName());
        assertTrue(domainQueue.isDurable());
        assertEquals("styleradar.estadisticas.dlq", dlq.getName());
        assertTrue(dlq.isDurable());
        assertEquals("", domainQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("styleradar.estadisticas.dlq", domainQueue.getArguments().get("x-dead-letter-routing-key"));

        List<String> routingKeys = bindings.getDeclarablesByType(Binding.class).stream()
                .map(Binding::getRoutingKey)
                .toList();
        assertEquals(List.of(
                DomainEventMapper.USER_REGISTERED_ROUTING_KEY,
                DomainEventMapper.INVENTORY_UPDATED_ROUTING_KEY,
                DomainEventMapper.GARMENT_PUBLISHED_ROUTING_KEY,
                DomainEventMapper.PLAYLIST_CREATED_ROUTING_KEY,
                DomainEventMapper.SUBSCRIPTION_PAYMENT_CONFIRMED_ROUTING_KEY,
                DomainEventMapper.SUBSCRIPTION_PLAN_UPDATED_ROUTING_KEY,
                DomainEventMapper.DONATION_COMPLETED_ROUTING_KEY), routingKeys);
    }

    private DomainEventPayload payload(String eventId, String eventType) {
        return new DomainEventPayload(
                eventId, eventType, "2026-11-21T14:00:00Z", "store-1", null, null, BigDecimal.ONE, 1L, null, null);
    }
}
