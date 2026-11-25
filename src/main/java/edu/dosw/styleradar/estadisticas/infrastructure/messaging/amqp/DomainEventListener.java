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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Component
public class DomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventListener.class);

    private final DomainEventMapper domainEventMapper;
    private final ProcessDomainEventUseCase processDomainEventUseCase;
    private final ProcessedEventJpaRepository processedEventRepository;
    private final TransactionOperations transactionOperations;

    @Autowired
    public DomainEventListener(
            DomainEventMapper domainEventMapper,
            ProcessDomainEventUseCase processDomainEventUseCase,
            ProcessedEventJpaRepository processedEventRepository,
            PlatformTransactionManager transactionManager
    ) {
        this(domainEventMapper, processDomainEventUseCase, processedEventRepository,
                new TransactionTemplate(transactionManager));
    }

    DomainEventListener(
            DomainEventMapper domainEventMapper,
            ProcessDomainEventUseCase processDomainEventUseCase,
            ProcessedEventJpaRepository processedEventRepository,
            TransactionOperations transactionOperations
    ) {
        this.domainEventMapper = domainEventMapper;
        this.processDomainEventUseCase = processDomainEventUseCase;
        this.processedEventRepository = processedEventRepository;
        this.transactionOperations = transactionOperations;
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
            transactionOperations.executeWithoutResult(status -> {
                processedEventRepository.saveAndFlush(
                        new ProcessedEventEntity(event.eventId(), event.eventType().name(), Instant.now()));
                processDomainEventUseCase.process(event);
            });
        } catch (DataIntegrityViolationException ex) {
            log.info("Evento de dominio duplicado concurrente descartado por idempotencia: eventoId={}", event.eventId());
        } catch (IllegalArgumentException ex) {
            throw new AmqpRejectAndDontRequeueException("Unprocessable domain analytics event: " + event.eventType(), ex);
        }
    }
}
