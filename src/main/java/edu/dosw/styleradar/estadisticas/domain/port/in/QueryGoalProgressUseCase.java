package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.GoalProgress;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;

import java.math.BigDecimal;

public interface QueryGoalProgressUseCase {
    GoalProgress query(MetricCode metricCode, Scope scope);

    default GoalProgress defineGoal(MetricCode metricCode, Scope scope, BigDecimal targetValue) {
        throw new UnsupportedOperationException("Goal definition is not supported");
    }
}
