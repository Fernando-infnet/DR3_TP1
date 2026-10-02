package com.exemplo.authservice.service;

import com.exemplo.authservice.dto.TokenResponse;
import com.exemplo.authservice.model.Usuario;
import com.exemplo.authservice.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Emite o par access token + refresh token e renova tokens a partir de um refresh token.
 * <p>
 * Os dois tokens sao JWT assinados com a mesma chave; o claim {@code token_type}
 * diferencia um do outro, impedindo que um refresh token seja usado para acessar
 * rotas protegidas (e vice-versa).
 */
@Service
public class TokenService {

    public static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String CLAIM_ROLES = "roles";
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final UsuarioRepository usuarioRepository;
    private final String issuer;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public TokenService(JwtEncoder jwtEncoder,
                        JwtDecoder jwtDecoder,
                        UsuarioRepository usuarioRepository,
                        @Value("${jwt.issuer}") String issuer,
                        @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
                        @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.usuarioRepository = usuarioRepository;
        this.issuer = issuer;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public TokenResponse gerarTokens(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Usuario nao encontrado"));

        Instant agora = Instant.now();

        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(usuario.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(accessTokenExpiration))
                .claim(CLAIM_TOKEN_TYPE, ACCESS)
                .claim(CLAIM_ROLES, usuario.getRolesList())
                .build();

        // O refresh token nao carrega perfis: eles sao relidos do banco a cada renovacao.
        JwtClaimsSet refreshClaims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(usuario.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(refreshTokenExpiration))
                .claim(CLAIM_TOKEN_TYPE, REFRESH)
                .build();

        return new TokenResponse(assinar(accessClaims), assinar(refreshClaims), "Bearer", accessTokenExpiration);
    }

    public TokenResponse renovar(String refreshToken) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(refreshToken);
        } catch (JwtException e) {
            throw new BadCredentialsException("Refresh token invalido ou expirado", e);
        }
        if (!REFRESH.equals(jwt.getClaimAsString(CLAIM_TOKEN_TYPE))) {
            throw new BadCredentialsException("O token informado nao e um refresh token");
        }
        return gerarTokens(jwt.getSubject());
    }

    private String assinar(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
