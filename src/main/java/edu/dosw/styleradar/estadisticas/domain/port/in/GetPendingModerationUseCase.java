package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.ModerationDecision;
import edu.dosw.styleradar.estadisticas.domain.model.ModerationDecisionResult;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModeration;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;

public interface GetPendingModerationUseCase {
    PendingModeration query(Scope scope);

    default ModerationDecisionResult decide(
            Scope scope,
            CategoryCode publisherType,
            String targetId,
            ModerationDecision decision
    ) {
        throw new UnsupportedOperationException("Moderation decisions are not supported");
    }
}
