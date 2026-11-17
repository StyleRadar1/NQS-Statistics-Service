package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.exception.UnsupportedMetricScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.RealTimeCount;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.port.in.QueryRealTimeCountUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.RealTimeCounterPort;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class QueryRealTimeCountUseCaseImpl implements QueryRealTimeCountUseCase {

    private final RealTimeCounterPort realTimeCounter;

    public QueryRealTimeCountUseCaseImpl(RealTimeCounterPort realTimeCounter) {
        this.realTimeCounter = realTimeCounter;
    }

    @Override
    public RealTimeCount query(MetricCode metricCode, Scope scope) {
        Objects.requireNonNull(metricCode, "Metric code must not be null");
        Objects.requireNonNull(scope, "Authenticated scope must not be null");
        if (!metricCode.isRealTime()) {
            throw new UnsupportedMetricScopeException(
                    "Metric " + metricCode + " does not support real-time counts");
        }
        if (metricCode.scopeType() != scope.type()) {
            throw new UnauthorizedScopeException("The authenticated scope cannot access this metric");
        }

        return new RealTimeCount(metricCode, scope, realTimeCounter.countActive(metricCode, scope));
    }
}
