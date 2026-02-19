package no.clueless.oauth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;
import java.util.Optional;

public class DefaultJwtGenerator implements TokenGenerator {
    private final Algorithm algorithm;
    private final String    issuer;
    private final long      accessTokenValiditySeconds;

    public DefaultJwtGenerator(Algorithm algorithm, String issuer, long accessTokenValiditySeconds) {
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm cannot be null");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("issuer cannot be null or blank");
        }
        if (accessTokenValiditySeconds <= 0) {
            throw new IllegalArgumentException("accessTokenValiditySeconds must be greater than 0");
        }
        this.algorithm                  = algorithm;
        this.issuer                     = issuer;
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    @Override
    public String generate(OAuthClient client) {
        if (client == null) {
            throw new IllegalArgumentException("client cannot be null");
        }
        if (!client.isEnabled()) {
            throw new IllegalArgumentException("client is not enabled");
        }
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(client.clientId())
                .withClaim("scope", Optional.ofNullable(client.scopes()).map(scopes -> String.join(" ", scopes)).orElse(null))
                .withExpiresAt(new Date(System.currentTimeMillis() + (accessTokenValiditySeconds * 1000)))
                .sign(algorithm);
    }
}
