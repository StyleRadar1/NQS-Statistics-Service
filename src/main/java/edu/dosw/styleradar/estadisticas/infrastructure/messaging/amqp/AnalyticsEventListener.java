package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import edu.dosw.styleradar.estadisticas.domain.model.AnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.port.in.RegisterAnalyticsEventUseCase;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.entity.ProcessedEventEntity;
import edu.dosw.styleradar.estadisticas.infrastructure.adapter.out.persistence.repository.ProcessedEventJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AnalyticsEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventListener.class);
    private static final String ALERTA_CREADA_EVENT_TYPE = "AlertaCreada";

    private final EventToMetricMapper eventToMetricMapper;
    private final RegisterAnalyticsEventUseCase registerAnalyticsEventUseCase;
    private final ProcessedEventJpaRepository processedEventRepository;

    @Autowired
    public AnalyticsEventListener(
            EventToMetricMapper eventToMetricMapper,
            RegisterAnalyticsEventUseCase registerAnalyticsEventUseCase,
            ProcessedEventJpaRepository processedEventRepository
    ) {
        this.eventToMetricMapper = eventToMetricMapper;
        this.registerAnalyticsEventUseCase = registerAnalyticsEventUseCase;
        this.processedEventRepository = processedEventRepository;
    }

    @RabbitListener(
            queues = RabbitMqConfig.ALERTA_CREADA_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void receive(AlertaCreadaEventPayload payload) {
        final AnalyticsEvent event;
        try {
            event = eventToMetricMapper.toAnalyticsEvent(payload);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new AmqpRejectAndDontRequeueException("Invalid AlertaCreada payload", ex);
        }

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Evento duplicado descartado por idempotencia: eventoId={}", event.eventId());
            return;
        }

        try {
            processedEventRepository.saveAndFlush(
                    new ProcessedEventEntity(event.eventId(), ALERTA_CREADA_EVENT_TYPE, Instant.now()));
        } catch (DataIntegrityViolationException ex) {
            log.info("Evento duplicado concurrente descartado por idempotencia: eventoId={}", event.eventId());
            return;
        }

        registerAnalyticsEventUseCase.register(event);
    }
}
