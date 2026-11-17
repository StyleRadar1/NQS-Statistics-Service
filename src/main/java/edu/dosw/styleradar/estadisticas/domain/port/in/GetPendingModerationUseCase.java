package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.PendingModeration;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;

public interface GetPendingModerationUseCase {
    PendingModeration query(Scope scope);
}
