package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import edu.dosw.styleradar.estadisticas.domain.port.in.RegisterAnalyticsEventUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsEventListener {

    private final EventToMetricMapper eventToMetricMapper;
    private final RegisterAnalyticsEventUseCase registerAnalyticsEventUseCase;

    public AnalyticsEventListener(
            EventToMetricMapper eventToMetricMapper,
            RegisterAnalyticsEventUseCase registerAnalyticsEventUseCase
    ) {
        this.eventToMetricMapper = eventToMetricMapper;
        this.registerAnalyticsEventUseCase = registerAnalyticsEventUseCase;
    }

    @RabbitListener(queues = RabbitMqConfig.ANALYTICS_QUEUE)
    public void receive(AnalyticsEventPayload payload) {
        registerAnalyticsEventUseCase.register(eventToMetricMapper.toAnalyticsEvent(payload));
    }
}
