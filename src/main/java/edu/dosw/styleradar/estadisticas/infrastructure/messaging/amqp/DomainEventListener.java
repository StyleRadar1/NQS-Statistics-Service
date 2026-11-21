package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import edu.dosw.styleradar.estadisticas.domain.model.DomainAnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.port.in.ProcessDomainEventUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.ProcessedEventEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.ProcessedEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventListener.class);

    private final DomainEventMapper domainEventMapper;
    private final ProcessDomainEventUseCase processDomainEventUseCase;
    private final ProcessedEventJpaRepository processedEventRepository;

    public DomainEventListener(
            DomainEventMapper domainEventMapper,
            ProcessDomainEventUseCase processDomainEventUseCase,
            ProcessedEventJpaRepository processedEventRepository
    ) {
        this.domainEventMapper = domainEventMapper;
        this.processDomainEventUseCase = processDomainEventUseCase;
        this.processedEventRepository = processedEventRepository;
    }

    @RabbitListener(
            queues = RabbitMqConfig.DOMAIN_EVENTS_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void receive(
            DomainEventPayload payload,
            @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey
    ) {
        final DomainAnalyticsEvent event;
        try {
            event = domainEventMapper.toDomainEvent(payload, routingKey);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new AmqpRejectAndDontRequeueException("Invalid domain analytics event payload", ex);
        }

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Evento de dominio duplicado descartado por idempotencia: eventoId={}", event.eventId());
            return;
        }

        try {
            processedEventRepository.saveAndFlush(
                    new ProcessedEventEntity(event.eventId(), event.eventType().name(), Instant.now()));
        } catch (DataIntegrityViolationException ex) {
            log.info("Evento de dominio duplicado concurrente descartado por idempotencia: eventoId={}", event.eventId());
            return;
        }

        try {
            processDomainEventUseCase.process(event);
        } catch (IllegalArgumentException ex) {
            throw new AmqpRejectAndDontRequeueException("Unprocessable domain analytics event: " + event.eventType(), ex);
        }
    }
}
