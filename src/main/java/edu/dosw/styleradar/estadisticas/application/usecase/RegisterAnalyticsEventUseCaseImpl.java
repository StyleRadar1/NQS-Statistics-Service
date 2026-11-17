package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.model.AnalyticsEvent;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.PaymentStatus;
import edu.dosw.styleradar.estadisticas.domain.port.in.RegisterAnalyticsEventUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.RealTimeCounterPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

@Service
public class RegisterAnalyticsEventUseCaseImpl implements RegisterAnalyticsEventUseCase {

    private static final Duration EVENT_IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final RealTimeCounterPort realTimeCounter;
    private final Clock clock;

    public RegisterAnalyticsEventUseCaseImpl(RealTimeCounterPort realTimeCounter, Clock clock) {
        this.realTimeCounter = realTimeCounter;
        this.clock = clock;
    }

    @Override
    public void register(AnalyticsEvent event) {
        Objects.requireNonNull(event, "Analytics event must not be null");
        if (!realTimeCounter.markEventProcessed(event.eventId(), EVENT_IDEMPOTENCY_TTL)) {
            return;
        }

        if (event.metricCode() == MetricCode.PLATFORM_SUBSCRIPTION_REVENUE
                && event.paymentStatus() != PaymentStatus.APROBADO) {
            return;
        }

        if (event.metricCode().isRealTime()) {
            realTimeCounter.incrementActive(
                    event.metricCode(),
                    event.scope(),
                    event.value().longValueExact());
        }
        realTimeCounter.incrementDaily(
                event.metricCode(),
                event.scope(),
                event.occurredAt().atZone(clock.getZone()).toLocalDate(),
                event.value());
    }
}
