package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModeration;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModerationGroup;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModerationPublisher;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModerationPublisherCount;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.in.GetPendingModerationUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.PendingModerationRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class GetPendingModerationUseCaseImpl implements GetPendingModerationUseCase {

    private static final String EMPTY_RESULT_MESSAGE = "No hay publicaciones pendientes de moderación";
    private static final List<CategoryCode> PUBLISHER_TYPES =
            List.of(CategoryCode.ORIGIN_STORE, CategoryCode.ORIGIN_USER);

    private final PendingModerationRepositoryPort pendingModerationRepository;

    public GetPendingModerationUseCaseImpl(PendingModerationRepositoryPort pendingModerationRepository) {
        this.pendingModerationRepository = pendingModerationRepository;
    }

    @Override
    public PendingModeration query(Scope scope) {
        Objects.requireNonNull(scope, "Authenticated scope must not be null");
        if (scope.type() != ScopeType.PLATFORM) {
            throw new UnauthorizedScopeException("Only the platform administrator can query pending moderation");
        }

        List<PendingModerationPublisherCount> pendingPublishers =
                pendingModerationRepository.findPendingModerationPublishers(
                MetricCode.MODERATION_PUBLICATIONS,
                scope,
                PUBLISHER_TYPES);
        if (pendingPublishers.isEmpty()) {
            return new PendingModeration(List.of(), 0, EMPTY_RESULT_MESSAGE);
        }

        List<PendingModerationGroup> groups = PUBLISHER_TYPES.stream()
                .map(type -> new PendingModerationGroup(
                        type,
                        pendingPublishers.stream()
                                .filter(publisher -> publisher.publisherType().equals(type))
                                .map(publisher -> new PendingModerationPublisher(
                                        publisher.publisherId(),
                                        publisher.publisherName(),
                                        publisher.pendingCount()))
                                .toList()))
                .toList();
        long totalPendingCount = pendingPublishers.stream()
                .mapToLong(publisher -> publisher.pendingCount())
                .sum();

        return new PendingModeration(groups, totalPendingCount, null);
    }
}
