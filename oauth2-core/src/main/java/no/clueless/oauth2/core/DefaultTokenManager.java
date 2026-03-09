package no.clueless.oauth2.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultTokenManager implements TokenGenerator, TokenValidator {
    @NotNull
    private final Algorithm algorithm;
    @NotNull
    private final String    issuer;
    private final long      accessTokenValiditySeconds;

    public DefaultTokenManager(@NotNull Algorithm algorithm, @NotNull String issuer, long accessTokenValiditySeconds) {
        if (issuer.isBlank()) {
            throw new IllegalArgumentException("issuer cannot be blank");
        }
        if (accessTokenValiditySeconds <= 0) {
            throw new IllegalArgumentException("accessTokenValiditySeconds must be greater than 0");
        }
        this.algorithm                  = algorithm;
        this.issuer                     = issuer;
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    @Override
    public @NotNull String generate(@NotNull no.clueless.oauth2.core.OAuthClient client, @NotNull Set<String> scopes) {
        if (!client.isEnabled()) {
            throw new IllegalArgumentException("client is not enabled");
        }

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(client.clientId())
                .withClaim("scope", String.join(" ", scopes))
                .withExpiresAt(new Date(System.currentTimeMillis() + (accessTokenValiditySeconds * 1000)))
                .sign(algorithm);
    }

    @Override
    public @NotNull OAuthPrincipal validate(@NotNull String token) {
        if (token.isBlank()) {
            throw new IllegalArgumentException("token cannot be blank");
        }

        var decodedJWT = JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(token);

        var scopes = Optional.ofNullable(decodedJWT.getClaim("scope"))
                .map(Claim::asString)
                .filter(claim -> !claim.isBlank())
                .stream()
                .flatMap(scope -> Arrays.stream(scope.trim().split("\\s|,")))
                .collect(Collectors.toSet());

        return new OAuthPrincipal(decodedJWT.getSubject(), scopes, "client_credentials");
    }
}
