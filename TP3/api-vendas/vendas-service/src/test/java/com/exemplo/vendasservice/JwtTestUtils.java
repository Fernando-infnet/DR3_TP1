package com.exemplo.vendasservice;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Gera tokens JWT nos testes, com a mesma chave e os mesmos claims que o auth-service emite.
 */
public final class JwtTestUtils {

    public static final String SECRET = "chave-secreta-de-teste-com-pelo-menos-32-bytes-hs256";

    private static final NimbusJwtEncoder ENCODER = new NimbusJwtEncoder(
            new ImmutableSecret<>(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));

    private JwtTestUtils() {
    }

    public static String accessToken(String usuario, String... roles) {
        return token(usuario, "access", "auth-service", Instant.now().plusSeconds(300), roles);
    }

    public static String refreshToken(String usuario) {
        return token(usuario, "refresh", "auth-service", Instant.now().plusSeconds(3600));
    }

    public static String tokenExpirado(String usuario, String... roles) {
        return token(usuario, "access", "auth-service", Instant.now().minusSeconds(120), roles);
    }

    public static String tokenDeOutroEmissor(String usuario, String... roles) {
        return token(usuario, "access", "emissor-desconhecido", Instant.now().plusSeconds(300), roles);
    }

    private static String token(String usuario, String tipo, String issuer, Instant expiraEm, String... roles) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(usuario)
                .issuedAt(expiraEm.minusSeconds(300))
                .expiresAt(expiraEm)
                .claim("token_type", tipo);
        if (roles.length > 0) {
            claims.claim("roles", List.of(roles));
        }
        return ENCODER.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }
}
