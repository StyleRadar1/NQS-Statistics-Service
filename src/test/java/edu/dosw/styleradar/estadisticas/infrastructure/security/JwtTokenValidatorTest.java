package edu.dosw.styleradar.estadisticas.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.dosw.styleradar.estadisticas.domain.exception.UnauthorizedScopeException;
import edu.dosw.styleradar.estadisticas.domain.model.MetricCode;
import edu.dosw.styleradar.estadisticas.domain.model.Scope;
import edu.dosw.styleradar.estadisticas.domain.model.ScopeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenValidatorTest {

    private static final String TEST_SECRET = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    private static final String OTHER_SECRET = Base64.getEncoder()
            .encodeToString("fedcba9876543210fedcba9876543210".getBytes(StandardCharsets.UTF_8));
    private static final Instant NOW = Instant.parse("2026-11-17T15:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtTokenValidator validator = new JwtTokenValidator(TEST_SECRET, objectMapper, FIXED_CLOCK);
    private final AuthenticatedScopeResolver scopeResolver = new AuthenticatedScopeResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validatesHs256TokensWithOfficialRolesAndSubFallback() throws Exception {
        String storeToken = signToken(TEST_SECRET, Map.of(
                "sub", "store-sub-1",
                "roles", List.of("ADMIN_ALMACEN"),
                "exp", NOW.plusSeconds(300).getEpochSecond()));
        AuthenticatedActor storeActor = validator.validateAndExtract(storeToken);
        assertEquals("STORE_ADMIN", storeActor.role());
        assertEquals("store-sub-1", storeActor.storeId());

        String explicitStoreToken = signToken(TEST_SECRET, Map.of(
                "sub", "user-123",
                "roles", List.of("ADMIN_ALMACEN"),
                "storeId", "store-explicit-9"));
        assertEquals("store-explicit-9", validator.validateAndExtract(explicitStoreToken).storeId());

        String foundationToken = signToken(TEST_SECRET, Map.of(
                "sub", "foundation-sub-2",
                "roles", List.of("REPRESENTANTE_FUNDACION")));
        AuthenticatedActor foundationActor = validator.validateAndExtract(foundationToken);
        assertEquals("FOUNDATION_ADMIN", foundationActor.role());
        assertEquals("foundation-sub-2", foundationActor.foundationId());

        String adminToken = signToken(TEST_SECRET, Map.of(
                "sub", "admin-1",
                "roles", List.of("ADMIN_STYLERADAR")));
        assertEquals("PLATFORM_ADMIN", validator.validateAndExtract(adminToken).role());

        String buyerToken = signToken(TEST_SECRET, Map.of(
                "sub", "buyer-1",
                "roles", List.of("COMPRADOR")));
        assertEquals("COMPRADOR", validator.validateAndExtract(buyerToken).role());

        String srAdminToken = signToken(TEST_SECRET, Map.of(
                "sub", "admin-sr-1",
                "roles", List.of("STYLERADAR_ADMIN")));
        assertEquals("PLATFORM_ADMIN", validator.validateAndExtract(srAdminToken).role());

        String srFoundationToken = signToken(TEST_SECRET, Map.of(
                "sub", "fund-sr-1",
                "roles", List.of("FOUNDATION_REPRESENTATIVE")));
        AuthenticatedActor srFoundationActor = validator.validateAndExtract(srFoundationToken);
        assertEquals("FOUNDATION_ADMIN", srFoundationActor.role());
        assertEquals("fund-sr-1", srFoundationActor.foundationId());

        String srBuyerToken = signToken(TEST_SECRET, Map.of(
                "sub", "buyer-sr-1",
                "roles", List.of("BUYER")));
        assertEquals("COMPRADOR", validator.validateAndExtract(srBuyerToken).role());

        String nitStoreToken = signToken(TEST_SECRET, Map.of(
                "sub", "user-77",
                "roles", List.of("STORE_ADMIN"),
                "nit", "900123456-1"));
        assertEquals("900123456-1", validator.validateAndExtract(nitStoreToken).storeId());

        String storeAliasToken = signToken(TEST_SECRET, Map.of(
                "sub", "user-88",
                "roles", List.of("STORE"),
                "almacenId", "store-alias-88"));
        AuthenticatedActor storeAliasActor = validator.validateAndExtract(storeAliasToken);
        assertEquals("STORE_ADMIN", storeAliasActor.role());
        assertEquals("store-alias-88", storeAliasActor.storeId());

        String tiendaAliasToken = signToken(TEST_SECRET, Map.of(
                "sub", "user-89",
                "roles", List.of("TIENDA"),
                "almacenId", "store-tienda-89"));
        assertEquals("STORE_ADMIN", validator.validateAndExtract(tiendaAliasToken).role());

        String fundacionAliasToken = signToken(TEST_SECRET, Map.of(
                "sub", "user-90",
                "roles", List.of("FOUNDATION"),
                "fundacionId", "fund-90"));
        assertEquals("fund-90", validator.validateAndExtract(fundacionAliasToken).foundationId());
    }

    @Test
    void rejectsWrongSecretExpiredMalformedOrUnconfiguredJwt() throws Exception {
        String wrongSecretToken = signToken(OTHER_SECRET, Map.of(
                "sub", "store-1",
                "roles", List.of("ADMIN_ALMACEN")));
        assertThrows(JwtTokenValidator.InvalidJwtException.class,
                () -> validator.validateAndExtract(wrongSecretToken));

        String expiredToken = signToken(TEST_SECRET, Map.of(
                "sub", "store-1",
                "roles", List.of("ADMIN_ALMACEN"),
                "exp", NOW.minusSeconds(1).getEpochSecond()));
        assertThrows(JwtTokenValidator.InvalidJwtException.class,
                () -> validator.validateAndExtract(expiredToken));

        String futureNbfToken = signToken(TEST_SECRET, Map.of(
                "sub", "store-1",
                "roles", List.of("ADMIN_ALMACEN"),
                "nbf", NOW.plusSeconds(60).getEpochSecond()));
        assertThrows(JwtTokenValidator.InvalidJwtException.class,
                () -> validator.validateAndExtract(futureNbfToken));

        String missingSubToken = signToken(TEST_SECRET, Map.of("roles", List.of("ADMIN_ALMACEN")));
        assertThrows(JwtTokenValidator.InvalidJwtException.class,
                () -> validator.validateAndExtract(missingSubToken));

        String unknownRoleToken = signToken(TEST_SECRET, Map.of(
                "sub", "user-1",
                "roles", List.of("INVITED")));
        assertThrows(JwtTokenValidator.InvalidJwtException.class,
                () -> validator.validateAndExtract(unknownRoleToken));

        assertThrows(JwtTokenValidator.InvalidJwtException.class, () -> validator.validateAndExtract("not-a-jwt"));
        assertThrows(JwtTokenValidator.InvalidJwtException.class, () -> validator.validateAndExtract(""));

        JwtTokenValidator unconfigured = new JwtTokenValidator("", objectMapper, FIXED_CLOCK);
        assertThrows(JwtTokenValidator.InvalidJwtException.class,
                () -> unconfigured.validateAndExtract(wrongSecretToken));
    }

    @Test
    void filterAuthenticatesValidBearerTokenAndRejectsInvalidWith401() throws Exception {
        GatewayHeaderAuthenticationFilter filter = new GatewayHeaderAuthenticationFilter(validator);

        String validToken = signToken(TEST_SECRET, Map.of(
                "sub", "store-44",
                "roles", List.of("ADMIN_ALMACEN"),
                "exp", NOW.plusSeconds(300).getEpochSecond()));
        MockHttpServletRequest validReq = new MockHttpServletRequest();
        validReq.addHeader("Authorization", "Bearer " + validToken);
        MockHttpServletResponse validRes = new MockHttpServletResponse();
        filter.doFilter(validReq, validRes, new MockFilterChain());

        assertEquals(200, validRes.getStatus());
        assertEquals(new Scope("store-44", ScopeType.STORE), scopeResolver.resolve(MetricCode.STORE_INVENTORY_BY_STATE));

        SecurityContextHolder.clearContext();
        String invalidToken = signToken(OTHER_SECRET, Map.of(
                "sub", "store-44",
                "roles", List.of("ADMIN_ALMACEN")));
        MockHttpServletRequest invalidReq = new MockHttpServletRequest();
        invalidReq.setRequestURI("/api/v1/statistics/distributions/STORE_INVENTORY_BY_STATE");
        invalidReq.addHeader("Authorization", "Bearer " + invalidToken);
        MockHttpServletResponse invalidRes = new MockHttpServletResponse();
        filter.doFilter(invalidReq, invalidRes, new MockFilterChain());

        assertEquals(401, invalidRes.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(invalidRes.getContentAsString().contains("\"status\":401"));

        String buyerToken = signToken(TEST_SECRET, Map.of(
                "sub", "buyer-9",
                "roles", List.of("COMPRADOR")));
        MockHttpServletRequest buyerReq = new MockHttpServletRequest();
        buyerReq.addHeader("Authorization", "Bearer " + buyerToken);
        filter.doFilter(buyerReq, new MockHttpServletResponse(), new MockFilterChain());
        assertThrows(UnauthorizedScopeException.class, () -> scopeResolver.resolve(MetricCode.STORE_INVENTORY_BY_STATE));
    }

    @Test
    void jwtClaimCannotBeOverriddenByHeadersButFallsBackWhenClaimIsNull() throws Exception {
        GatewayHeaderAuthenticationFilter filter = new GatewayHeaderAuthenticationFilter(validator);

        String claimStoreToken = signToken(TEST_SECRET, Map.of(
                "sub", "user-10",
                "roles", List.of("STORE"),
                "storeId", "store-jwt-10"));
        MockHttpServletRequest idorAttemptReq = new MockHttpServletRequest();
        idorAttemptReq.addHeader("Authorization", "Bearer " + claimStoreToken);
        idorAttemptReq.addHeader("X-Store-Id", "victim-store-99");
        filter.doFilter(idorAttemptReq, new MockHttpServletResponse(), new MockFilterChain());
        assertEquals(new Scope("store-jwt-10", ScopeType.STORE),
                scopeResolver.resolve(MetricCode.STORE_INVENTORY_BY_STATE));

        SecurityContextHolder.clearContext();
        String noStoreClaimToken = signToken(TEST_SECRET, Map.of(
                "sub", "user-20",
                "roles", List.of("TIENDA")));
        MockHttpServletRequest gatewayFallbackReq = new MockHttpServletRequest();
        gatewayFallbackReq.addHeader("Authorization", "Bearer " + noStoreClaimToken);
        gatewayFallbackReq.addHeader("X-Store-Id", "store-gw-20");
        filter.doFilter(gatewayFallbackReq, new MockHttpServletResponse(), new MockFilterChain());
        assertEquals(new Scope("store-gw-20", ScopeType.STORE),
                scopeResolver.resolve(MetricCode.STORE_INVENTORY_BY_STATE));
    }

    private String signToken(String base64Secret, Map<String, Object> claims) throws Exception {
        String headerJson = objectMapper.writeValueAsString(Map.of("alg", "HS256", "typ", "JWT"));
        String payloadJson = objectMapper.writeValueAsString(claims);
        Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
        String signingInput = urlEncoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8))
                + "."
                + urlEncoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(Base64.getDecoder().decode(base64Secret), "HmacSHA256"));
        String signature = urlEncoder.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));
        return signingInput + "." + signature;
    }
}
