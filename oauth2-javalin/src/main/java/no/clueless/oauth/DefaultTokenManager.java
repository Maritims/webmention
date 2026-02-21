package no.clueless.oauth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The default implementation of the {@link TokenGenerator} and {@link TokenGenerator} interfaces.
 */
public class DefaultTokenManager implements TokenGenerator, TokenValidator {
    private final Algorithm algorithm;
    private final String    issuer;
    private final long      accessTokenValiditySeconds;

    /**
     * Constructor.
     *
     * @param algorithm                  The algorithm to use for signing the JWT.
     * @param issuer                     The issuer to use for the JWT.
     * @param accessTokenValiditySeconds The number of seconds the JWT should be valid for.
     * @throws IllegalArgumentException If the algorithm is null, the issuer is null or blank, or accessTokenValiditySeconds is less than or equal to 0.
     */
    public DefaultTokenManager(Algorithm algorithm, String issuer, long accessTokenValiditySeconds) {
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

    /**
     * Generates a JWT for the given client.
     *
     * @param client The client to generate a token for.
     * @param scopes The scopes to include in the token.
     * @return The generated JWT.
     * @throws IllegalArgumentException If the client is null or not enabled.
     */
    @Override
    public String generate(OAuthClient client, Set<Scope> scopes) {
        if (client == null) {
            throw new IllegalArgumentException("client cannot be null");
        }
        if (!client.isEnabled()) {
            throw new IllegalArgumentException("client is not enabled");
        }
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(client.clientId())
                .withClaim("scope", scopes.stream().map(Scope::getLabel).collect(Collectors.joining(" ")))
                .withExpiresAt(new Date(System.currentTimeMillis() + (accessTokenValiditySeconds * 1000)))
                .sign(algorithm);
    }

    /**
     * Validates the given token.
     *
     * @param token The token to validate.
     * @return The authenticated principal.
     * @throws IllegalArgumentException If the token is null or blank.
     */
    @Override
    public OAuthPrincipal validate(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token cannot be null or blank");
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
                .map(Scope::fromLabel)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        return new OAuthPrincipal(decodedJWT.getSubject(), scopes, "client_credentials");
    }
}
