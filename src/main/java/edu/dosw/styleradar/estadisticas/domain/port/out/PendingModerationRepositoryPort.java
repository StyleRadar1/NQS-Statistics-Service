package edu.dosw.styleradar.estadisticas.domain.port.out;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModerationPublisherCount;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;

import java.util.List;

public interface PendingModerationRepositoryPort {
    List<PendingModerationPublisherCount> findPendingModerationPublishers(
            MetricCode metricCode,
            Scope scope,
            List<CategoryCode> publisherTypes);
}
