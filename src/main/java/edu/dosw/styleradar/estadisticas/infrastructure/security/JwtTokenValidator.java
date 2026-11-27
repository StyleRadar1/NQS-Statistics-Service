package edu.dosw.styleradar.estadisticas.infrastructure.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final List<String> ROLE_PRIORITY = List.of(
            "PLATFORM_ADMIN",
            "STORE_ADMIN",
            "FOUNDATION_ADMIN",
            "COMPRADOR"
    );

    private final byte[] secretKeyBytes;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public JwtTokenValidator(
            @Value("${security.jwt.secret:}") String base64Secret,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.secretKeyBytes = decodeSecret(base64Secret);
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public AuthenticatedActor validateAndExtract(String token) {
        return validateAndExtract(token, null, null);
    }

    public AuthenticatedActor validateAndExtract(
            String token,
            String headerStoreId,
            String headerFoundationId
    ) {
        if (secretKeyBytes.length == 0) {
            throw new InvalidJwtException("JWT secret is not configured");
        }
        if (token == null || token.isBlank()) {
            throw new InvalidJwtException("JWT token is blank");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidJwtException("Malformed JWT token");
        }

        Map<String, Object> header = decodeJsonPart(parts[0], "header");
        Object alg = header.get("alg");
        if (!"HS256".equals(alg)) {
            throw new InvalidJwtException("Unsupported JWT algorithm");
        }

        verifySignature(parts[0] + "." + parts[1], parts[2]);

        Map<String, Object> claims = decodeJsonPart(parts[1], "payload");
        validateTimestamps(claims);

        String subject = trimToNull(asString(claims.get("sub")));
        if (subject == null) {
            throw new InvalidJwtException("JWT token is missing subject (sub)");
        }

        String role = resolveRole(claims);
        if (role == null) {
            throw new InvalidJwtException("JWT token does not contain a recognized role");
        }

        String claimStoreId = firstNonBlank(claims, "storeId", "tiendaId", "idTienda", "store_id", "almacenId", "nit");
        String claimFoundationId = firstNonBlank(claims, "foundationId", "idFundacion", "foundation_id", "fundacionId");

        String storeId = claimStoreId != null ? claimStoreId : trimToNull(headerStoreId);
        String foundationId = claimFoundationId != null ? claimFoundationId : trimToNull(headerFoundationId);

        if ("FOUNDATION_ADMIN".equals(role) && foundationId == null) {
            log.warn("JWT for FOUNDATION_REPRESENTATIVE sub={} lacks foundationId claim and X-Foundation-Id header", subject);
        }

        return new AuthenticatedActor(role, storeId, foundationId);
    }

    private void verifySignature(String signingInput, String encodedSignature) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKeyBytes, HMAC_SHA256));
            byte[] expected = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
            byte[] actual = Base64.getUrlDecoder().decode(encodedSignature);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new InvalidJwtException("Invalid JWT signature");
            }
        } catch (IllegalArgumentException | GeneralSecurityException ex) {
            throw new InvalidJwtException("Invalid JWT signature", ex);
        }
    }

    private Map<String, Object> decodeJsonPart(String encodedPart, String partName) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedPart);
            return objectMapper.readValue(decoded, MAP_TYPE);
        } catch (Exception ex) {
            throw new InvalidJwtException("Invalid JWT " + partName, ex);
        }
    }

    private void validateTimestamps(Map<String, Object> claims) {
        long nowSeconds = clock.instant().getEpochSecond();
        Long exp = asLong(claims.get("exp"));
        if (exp != null && nowSeconds >= exp) {
            throw new InvalidJwtException("JWT token has expired");
        }
        Long nbf = asLong(claims.get("nbf"));
        if (nbf != null && nowSeconds < nbf) {
            throw new InvalidJwtException("JWT token is not yet valid");
        }
    }

    private String resolveRole(Map<String, Object> claims) {
        Object rawRoles = claims.get("roles");
        if (rawRoles == null) {
            rawRoles = claims.get("role");
        }
        List<String> normalized = extractNormalizedRoles(rawRoles);
        for (String candidate : ROLE_PRIORITY) {
            if (normalized.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private List<String> extractNormalizedRoles(Object rawRoles) {
        if (rawRoles instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::asString)
                    .map(JwtTokenValidator::normalizeRoleName)
                    .filter(r -> r != null)
                    .toList();
        }
        if (rawRoles instanceof String single) {
            String normalized = normalizeRoleName(single);
            return normalized != null ? List.of(normalized) : List.of();
        }
        return List.of();
    }

    static String normalizeRoleName(String rawRole) {
        String trimmed = trimToNull(rawRole);
        if (trimmed == null) {
            return null;
        }
        if (trimmed.startsWith("ROLE_")) {
            trimmed = trimmed.substring("ROLE_".length());
        }
        return switch (trimmed.toUpperCase(Locale.ROOT)) {
            case "STORE_ADMIN", "ADMIN_ALMACEN", "STORE", "TIENDA" -> "STORE_ADMIN";
            case "FOUNDATION_ADMIN", "FOUNDATION_REPRESENTATIVE", "REPRESENTANTE_FUNDACION", "FOUNDATION", "FUNDACION" -> "FOUNDATION_ADMIN";
            case "PLATFORM_ADMIN", "STYLERADAR_ADMIN", "ADMIN_STYLERADAR", "ADMIN" -> "PLATFORM_ADMIN";
            case "COMPRADOR", "BUYER" -> "COMPRADOR";
            default -> null;
        };
    }

    private String firstNonBlank(Map<String, Object> claims, String... keys) {
        for (String key : keys) {
            String value = trimToNull(asString(claims.get(key)));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
                throw new InvalidJwtException("Invalid numeric claim in JWT");
            }
        }
        return null;
    }

    private static byte[] decodeSecret(String base64Secret) {
        String trimmed = trimToNull(base64Secret);
        if (trimmed == null) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(trimmed);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static class InvalidJwtException extends RuntimeException {
        public InvalidJwtException(String message) {
            super(message);
        }

        public InvalidJwtException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
