package edu.dosw.styleradar.estadisticas.infrastructure.security;

import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedScopeResolverTest {

    private final AuthenticatedScopeResolver resolver = new AuthenticatedScopeResolver();
    private final GatewayHeaderAuthenticationFilter filter = new GatewayHeaderAuthenticationFilter();

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesOnlyTheAuthenticatedStoreScope() {
        authenticate(new AuthenticatedActor("STORE_ADMIN", "store-17", null));

        assertEquals(
                new Scope("store-17", ScopeType.STORE),
                resolver.resolve(MetricCode.STORE_PAGE_VISITS));
        assertThrows(
                UnauthorizedScopeException.class,
                () -> resolver.resolve(MetricCode.PLATFORM_SEARCHES));
    }

    @Test
    void resolvesFoundationAndPlatformScopesAndRejectsInvalidScopes() {
        authenticate(new AuthenticatedActor("FOUNDATION_ADMIN", null, "foundation-42"));
        assertEquals(
                new Scope("foundation-42", ScopeType.FOUNDATION),
                resolver.resolve(MetricCode.FOUNDATION_GARMENTS_RECEIVED));
        assertThrows(
                UnauthorizedScopeException.class,
                () -> resolver.resolve(MetricCode.STORE_PAGE_VISITS));

        authenticate(new AuthenticatedActor("PLATFORM_ADMIN", null, null));
        assertEquals(
                new Scope("platform", ScopeType.PLATFORM),
                resolver.resolve(MetricCode.PLATFORM_SEARCHES));
        assertThrows(
                UnauthorizedScopeException.class,
                () -> resolver.resolve(MetricCode.FOUNDATION_GARMENTS_RECEIVED));

        authenticate(new AuthenticatedActor("STORE_ADMIN", "   ", null));
        assertThrows(
                UnauthorizedScopeException.class,
                () -> resolver.resolve(MetricCode.STORE_PAGE_VISITS));

        authenticate(new AuthenticatedActor("BUYER", "store-1", null));
        assertThrows(
                UnauthorizedScopeException.class,
                () -> resolver.resolve(MetricCode.STORE_PAGE_VISITS));
    }

    @Test
    void rejectsMissingOrInvalidAuthenticationInSecurityContext() {
        SecurityContextHolder.clearContext();
        assertThrows(
                UnauthorizedScopeException.class,
                () -> resolver.resolve(MetricCode.STORE_PAGE_VISITS));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("anonymous", null));
        assertThrows(
                UnauthorizedScopeException.class,
                () -> resolver.resolve(MetricCode.STORE_PAGE_VISITS));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("not-an-actor", null, java.util.List.of()));
        assertThrows(
                UnauthorizedScopeException.class,
                () -> resolver.resolve(MetricCode.STORE_PAGE_VISITS));
    }

    @Test
    void gatewayFilterPopulatesContextForValidHeadersAndIgnoresIncompleteOnes() throws Exception {
        MockHttpServletRequest storeReq = new MockHttpServletRequest();
        storeReq.addHeader("X-User-Role", "ROLE_store_admin");
        storeReq.addHeader("X-Store-Id", " store-99 ");
        filter.doFilter(storeReq, new MockHttpServletResponse(), new MockFilterChain());
        assertEquals(
                new Scope("store-99", ScopeType.STORE),
                resolver.resolve(MetricCode.STORE_PAGE_VISITS));

        SecurityContextHolder.clearContext();
        MockHttpServletRequest foundationReq = new MockHttpServletRequest();
        foundationReq.addHeader("X-User-Role", "FOUNDATION_ADMIN");
        foundationReq.addHeader("X-Foundation-Id", "foundation-7");
        filter.doFilter(foundationReq, new MockHttpServletResponse(), new MockFilterChain());
        assertEquals(
                new Scope("foundation-7", ScopeType.FOUNDATION),
                resolver.resolve(MetricCode.FOUNDATION_GARMENTS_RECEIVED));

        SecurityContextHolder.clearContext();
        MockHttpServletRequest platformReq = new MockHttpServletRequest();
        platformReq.addHeader("X-User-Role", "PLATFORM_ADMIN");
        filter.doFilter(platformReq, new MockHttpServletResponse(), new MockFilterChain());
        assertEquals(
                new Scope("platform", ScopeType.PLATFORM),
                resolver.resolve(MetricCode.PLATFORM_SEARCHES));

        SecurityContextHolder.clearContext();
        MockHttpServletRequest missingStoreReq = new MockHttpServletRequest();
        missingStoreReq.addHeader("X-User-Role", "STORE_ADMIN");
        filter.doFilter(missingStoreReq, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        MockHttpServletRequest unknownRoleReq = new MockHttpServletRequest();
        unknownRoleReq.addHeader("X-User-Role", "UNKNOWN_ROLE");
        filter.doFilter(unknownRoleReq, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private void authenticate(AuthenticatedActor actor) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(actor, null, java.util.List.of()));
    }
}
