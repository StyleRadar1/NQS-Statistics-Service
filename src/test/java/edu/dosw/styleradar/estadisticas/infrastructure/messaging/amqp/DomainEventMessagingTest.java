package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

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
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DomainEventMessagingTest {

    private final DomainEventMapper mapper = new DomainEventMapper();
    private final LocalDateTime now = LocalDateTime.of(2026, 11, 21, 14, 0, 0);

    @Test
    void mapsPayloadsByExplicitTypeAndByRoutingKeyAcrossAllProducerMicroservices() {
        DomainAnalyticsEvent payment = mapper.toDomainEvent(
                new DomainEventPayload(
                        "evt-pay-1", null, now, null, "PLAN_TIENDA", "PENDIENTE",
                        "TIENDA", null, null, new BigDecimal("89000"), null, "APPROVED"),
                "styleradar.pagos.suscripcion.pago.aprobado");

        assertEquals(DomainEventType.SUBSCRIPTION_PAYMENT_PROCESSED, payment.eventType());
        assertEquals(CategoryCode.PLAN_STORE_MONTHLY, payment.categoryCode());
        assertEquals(CategoryCode.PENDING, payment.previousCategoryCode());
        assertEquals(CategoryCode.ORIGIN_STORE, payment.origin());
        assertEquals(PaymentStatus.APROBADO, payment.paymentStatus());

        assertEquals(DomainEventType.USER_SESSION_CHANGED,
                mapper.toDomainEvent(payload("e1", null), "styleradar.usuarios.sesion.iniciada").eventType());
        assertEquals(DomainEventType.FOUNDATION_STATUS_CHANGED,
                mapper.toDomainEvent(payload("e2", null), "styleradar.usuarios.fundacion.activada").eventType());
        assertEquals(DomainEventType.STORE_VISIT_REGISTERED,
                mapper.toDomainEvent(payload("e3", null), "styleradar.tiendas.visita.registrada").eventType());
        assertEquals(DomainEventType.STORE_VERIFICATION_CHANGED,
                mapper.toDomainEvent(payload("e4", null), "styleradar.tiendas.verificacion.actualizada").eventType());
        assertEquals(DomainEventType.INVENTORY_STATE_CHANGED,
                mapper.toDomainEvent(payload("e5", null), "styleradar.inventario.stock.actualizado").eventType());
        assertEquals(DomainEventType.GARMENT_MODERATION_PENDING,
                mapper.toDomainEvent(payload("e6", null), "styleradar.catalogo.prenda.moderacion.pendiente").eventType());
        assertEquals(DomainEventType.GARMENT_PUBLISHED,
                mapper.toDomainEvent(payload("e7", null), "styleradar.catalogo.prenda.publicada").eventType());
        assertEquals(DomainEventType.CATALOG_SEARCH_EXECUTED,
                mapper.toDomainEvent(payload("e8", null), "styleradar.catalogo.busqueda.ejecutada").eventType());
        assertEquals(DomainEventType.PLAYLIST_CREATED,
                mapper.toDomainEvent(payload("e9", null), "styleradar.playlist.creada").eventType());
        assertEquals(DomainEventType.GARMENT_SAVED_IN_PLAYLIST,
                mapper.toDomainEvent(payload("e10", null), "styleradar.playlist.prenda.guardada").eventType());
        assertEquals(DomainEventType.STORE_MESSAGE_RECEIVED,
                mapper.toDomainEvent(payload("e11", null), "styleradar.chat.mensaje.recibido").eventType());
        assertEquals(DomainEventType.SUBSCRIPTION_PLAN_CHANGED,
                mapper.toDomainEvent(payload("e12", null), "styleradar.pagos.suscripcion.plan.actualizado").eventType());
        assertEquals(DomainEventType.DONATION_REQUEST_CREATED,
                mapper.toDomainEvent(payload("e13", null), "styleradar.donaciones.solicitud.creada").eventType());
        assertEquals(DomainEventType.DONATION_REQUEST_STATE_CHANGED,
                mapper.toDomainEvent(payload("e14", null), "styleradar.donaciones.solicitud.estado.actualizado").eventType());
        assertEquals(DomainEventType.DONATION_COMPLETED,
                mapper.toDomainEvent(payload("e15", null), "styleradar.donaciones.completada").eventType());

        DomainAnalyticsEvent explicit = mapper.toDomainEvent(
                new DomainEventPayload(
                        "e16", "DONATION_COMPLETED", now, "foundation-1", "APROBADA", null,
                        "PERSONA", null, null, BigDecimal.ONE, 1L, "REJECTED"),
                null);
        assertEquals(DomainEventType.DONATION_COMPLETED, explicit.eventType());
        assertEquals(CategoryCode.APPROVED, explicit.categoryCode());
        assertEquals(CategoryCode.ORIGIN_USER, explicit.origin());
        assertEquals(PaymentStatus.RECHAZADO, explicit.paymentStatus());

        DomainAnalyticsEvent pendingPayment = mapper.toDomainEvent(
                new DomainEventPayload(
                        "e17", "SUBSCRIPTION_PAYMENT_PROCESSED", now, null, "DRESS", null,
                        null, null, null, BigDecimal.ONE, null, "PENDING"),
                null);
        assertEquals(PaymentStatus.PENDIENTE, pendingPayment.paymentStatus());
        assertEquals(new CategoryCode("DRESS"), pendingPayment.categoryCode());
    }

    @Test
    void rejectsInvalidPayloadsAndUnknownRoutingKeys() {
        assertThrows(NullPointerException.class, () -> mapper.toDomainEvent(null, "styleradar.tiendas.visita"));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toDomainEvent(payload(" ", "GARMENT_PUBLISHED"), null));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toDomainEvent(payload("e1", "UNKNOWN_TYPE"), null));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toDomainEvent(payload("e1", null), "styleradar.desconocido.evento"));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toDomainEvent(payload("e1", null), null));
        assertThrows(IllegalArgumentException.class, () -> mapper.toDomainEvent(
                new DomainEventPayload("e1", "DONATION_COMPLETED", now, "f1", null, null, "INVALID",
                        null, null, BigDecimal.ONE, null, null),
                null));
    }

    @Test
    void listenerDeduplicatesEventsAndRoutesInvalidEventsToDlq() {
        ProcessDomainEventUseCase useCase = mock(ProcessDomainEventUseCase.class);
        ProcessedEventJpaRepository processedRepo = mock(ProcessedEventJpaRepository.class);
        DomainEventListener listener = new DomainEventListener(mapper, useCase, processedRepo);

        DomainEventPayload validPayload = payload("evt-dom-1", "PLAYLIST_CREATED");
        when(processedRepo.existsById("evt-dom-1")).thenReturn(false, true);

        listener.receive(validPayload, "styleradar.playlist.creada");
        listener.receive(validPayload, "styleradar.playlist.creada");

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
                .thenReturn(new ProcessedEventEntity("evt-dom-bad-usecase", "STORE_VISIT_REGISTERED", java.time.Instant.now()));
        doThrow(new IllegalArgumentException("scopeId required")).when(useCase).process(any());
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.receive(payload("evt-dom-bad-usecase", "STORE_VISIT_REGISTERED"), null));
    }

    @Test
    void configuresDomainEventsQueueDlqAndAllProducerBindings() {
        RabbitMqConfig config = new RabbitMqConfig();
        TopicExchange exchange = config.analyticsExchange();
        Queue domainQueue = config.domainEventsQueue();
        Declarables bindings = config.domainEventBindings(domainQueue, exchange);

        assertEquals("styleradar.estadisticas.eventos-dominio.q", domainQueue.getName());
        assertTrue(domainQueue.isDurable());
        assertEquals("", domainQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("styleradar.estadisticas.dlq", domainQueue.getArguments().get("x-dead-letter-routing-key"));

        List<String> routingKeys = bindings.getDeclarablesByType(Binding.class).stream()
                .map(Binding::getRoutingKey)
                .toList();
        assertEquals(List.of(
                "styleradar.usuarios.#",
                "styleradar.tiendas.#",
                "styleradar.inventario.#",
                "styleradar.catalogo.#",
                "styleradar.playlist.#",
                "styleradar.chat.#",
                "styleradar.pagos.#",
                "styleradar.donaciones.#"), routingKeys);
    }

    private DomainEventPayload payload(String eventId, String eventType) {
        return new DomainEventPayload(
                eventId, eventType, now, "store-1", null, null, null, null, null, BigDecimal.ONE, 1L, null);
    }
}
