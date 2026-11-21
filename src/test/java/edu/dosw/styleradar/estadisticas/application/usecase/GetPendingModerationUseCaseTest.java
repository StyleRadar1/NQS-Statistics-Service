package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.CategoryCode;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.ModerationDecision;
import edu.dosw.styleradar.estadisticas.domain.model.PendingModerationPublisherCount;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import edu.dosw.styleradar.estadisticas.domain.port.out.PendingModerationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetPendingModerationUseCaseTest {

    private static final Scope PLATFORM_SCOPE = new Scope("platform", ScopeType.PLATFORM);

    private final PendingModerationRepositoryPort repository = mock(PendingModerationRepositoryPort.class);
    private GetPendingModerationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPendingModerationUseCaseImpl(repository);
    }

    @Test
    void groupsPendingPublishersByStoreAndUserOrigin() {
        when(repository.findPendingModerationPublishers(
                MetricCode.MODERATION_PUBLICATIONS,
                PLATFORM_SCOPE,
                List.of(CategoryCode.ORIGIN_STORE, CategoryCode.ORIGIN_USER)))
                .thenReturn(List.of(
                        publisher(CategoryCode.ORIGIN_STORE, "store-1", "Store One", 3),
                        publisher(CategoryCode.ORIGIN_USER, "user-1", "User One", 2),
                        publisher(CategoryCode.ORIGIN_STORE, "store-2", "Store Two", 4)));

        var result = useCase.query(PLATFORM_SCOPE);

        assertEquals(9, result.totalPendingCount());
        assertEquals(2, result.groups().size());
        assertEquals(CategoryCode.ORIGIN_STORE, result.groups().get(0).publisherType());
        assertEquals(List.of("store-1", "store-2"),
                result.groups().get(0).publishers().stream().map(item -> item.publisherId()).toList());
        assertEquals(List.of(3L, 4L),
                result.groups().get(0).publishers().stream().map(item -> item.pendingCount()).toList());
        assertEquals(CategoryCode.ORIGIN_USER, result.groups().get(1).publisherType());
        assertEquals(List.of("user-1"),
                result.groups().get(1).publishers().stream().map(item -> item.publisherId()).toList());
        verify(repository).findPendingModerationPublishers(
                MetricCode.MODERATION_PUBLICATIONS,
                PLATFORM_SCOPE,
                List.of(CategoryCode.ORIGIN_STORE, CategoryCode.ORIGIN_USER));
    }

    @Test
    void returnsEmptyGroupsAndInformationalMessageWhenThereAreNoPendingPublications() {
        when(repository.findPendingModerationPublishers(
                MetricCode.MODERATION_PUBLICATIONS,
                PLATFORM_SCOPE,
                List.of(CategoryCode.ORIGIN_STORE, CategoryCode.ORIGIN_USER)))
                .thenReturn(List.of());

        var result = useCase.query(PLATFORM_SCOPE);

        assertEquals(List.of(), result.groups());
        assertEquals(0, result.totalPendingCount());
        org.junit.jupiter.api.Assertions.assertTrue(result.message().contains("No hay publicaciones pendientes"));
    }

    @Test
    void rejectsNonPlatformScopeWithoutQueryingRepository() {
        assertThrows(UnauthorizedScopeException.class,
                () -> useCase.query(new Scope("store-1", ScopeType.STORE)));
        assertThrows(UnauthorizedScopeException.class,
                () -> useCase.query(new Scope("foundation-1", ScopeType.FOUNDATION)));

        verify(repository, never()).findPendingModerationPublishers(
                MetricCode.MODERATION_PUBLICATIONS,
                new Scope("store-1", ScopeType.STORE),
                List.of(CategoryCode.ORIGIN_STORE, CategoryCode.ORIGIN_USER));
    }

    @Test
    void approvesAndRejectsPendingModerationAndReturnsUpdatedPendingList() {
        when(repository.resolvePendingByPublisher(
                MetricCode.MODERATION_PUBLICATIONS,
                PLATFORM_SCOPE,
                CategoryCode.ORIGIN_STORE,
                "store-1")).thenReturn(3L);
        when(repository.findPendingModerationPublishers(
                MetricCode.MODERATION_PUBLICATIONS,
                PLATFORM_SCOPE,
                List.of(CategoryCode.ORIGIN_STORE, CategoryCode.ORIGIN_USER)))
                .thenReturn(List.of(publisher(CategoryCode.ORIGIN_USER, "user-1", "User One", 2)));

        var approved = useCase.decide(
                PLATFORM_SCOPE,
                CategoryCode.ORIGIN_STORE,
                " store-1 ",
                ModerationDecision.APROBAR);

        assertEquals(CategoryCode.ORIGIN_STORE, approved.publisherType());
        assertEquals("store-1", approved.targetId());
        assertEquals("APROBADA", approved.resultadoModeracion());
        assertEquals(3L, approved.moderatedCount());
        assertEquals(2L, approved.updatedPending().totalPendingCount());

        when(repository.resolvePendingByPublisher(
                MetricCode.MODERATION_PUBLICATIONS,
                PLATFORM_SCOPE,
                CategoryCode.ORIGIN_USER,
                "user-1")).thenReturn(2L);
        when(repository.findPendingModerationPublishers(
                MetricCode.MODERATION_PUBLICATIONS,
                PLATFORM_SCOPE,
                List.of(CategoryCode.ORIGIN_STORE, CategoryCode.ORIGIN_USER)))
                .thenReturn(List.of());

        var rejected = useCase.decide(
                PLATFORM_SCOPE,
                CategoryCode.ORIGIN_USER,
                "user-1",
                ModerationDecision.RECHAZAR);

        assertEquals("RECHAZADA", rejected.resultadoModeracion());
        assertEquals(0L, rejected.updatedPending().totalPendingCount());
    }

    @Test
    void rejectsInvalidModerationDecisionRequests() {
        assertThrows(UnauthorizedScopeException.class, () -> useCase.decide(
                new Scope("store-1", ScopeType.STORE),
                CategoryCode.ORIGIN_STORE,
                "store-1",
                ModerationDecision.APROBAR));

        assertThrows(IllegalArgumentException.class, () -> useCase.decide(
                PLATFORM_SCOPE,
                CategoryCode.AVAILABLE,
                "store-1",
                ModerationDecision.APROBAR));

        assertThrows(IllegalArgumentException.class, () -> useCase.decide(
                PLATFORM_SCOPE,
                CategoryCode.ORIGIN_STORE,
                "  ",
                ModerationDecision.APROBAR));

        when(repository.resolvePendingByPublisher(
                MetricCode.MODERATION_PUBLICATIONS,
                PLATFORM_SCOPE,
                CategoryCode.ORIGIN_STORE,
                "missing-store")).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> useCase.decide(
                PLATFORM_SCOPE,
                CategoryCode.ORIGIN_STORE,
                "missing-store",
                ModerationDecision.RECHAZAR));
    }

    private PendingModerationPublisherCount publisher(
            CategoryCode type,
            String id,
            String name,
            long count
    ) {
        return new PendingModerationPublisherCount(type, id, name, count);
    }
}
