package no.clueless.webmention.service;

import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import no.clueless.oauth.*;
import no.clueless.webmention.WebmentionEndpointDiscoverer;
import no.clueless.webmention.event.WebmentionEvent;
import no.clueless.webmention.event.WebmentionReceivedSubscriber;
import no.clueless.webmention.http.SecureHttpClient;
import no.clueless.webmention.notifier.email.WebmentionEmailViaResendNotifier;
import no.clueless.webmention.persistence.WebmentionRepository;
import no.clueless.webmention.persistence.sqlite.SqliteClientStore;
import no.clueless.webmention.persistence.sqlite.SqliteWebmentionRepository;
import no.clueless.webmention.receiver.*;
import no.clueless.webmention.javalin.WebmentionPlugin;

import java.time.Duration;
import java.util.concurrent.SubmissionPublisher;

public class Application {
    public static void main(String[] args) {
        final var options                      = Options.fromEnvironment();
        final var httpClient                   = SecureHttpClient.newClient(Duration.ofMillis(options.connectTimeout()), !options.testMode());
        final var webmentionEndpointDiscoverer = new WebmentionEndpointDiscoverer(httpClient);
        final var targetVerifier               = new DefaultWebmentionTargetVerifier(options.supportedDomains(), httpClient, webmentionEndpointDiscoverer);
        final var webmentionRepository         = new SqliteWebmentionRepository(options.databaseConnectionString()).initialize();
        final var onWebmentionReceived         = new SubmissionPublisher<WebmentionEvent>();
        final var receiver                     = new WebmentionReceiver(httpClient, new WebmentionRequestVerifier(targetVerifier), onWebmentionReceived);
        final var webmentionProcessor          = new WebmentionProcessor(new WebmentionRateLimiter(5000, 5), receiver);
        final var webmentionNotifier           = new WebmentionEmailViaResendNotifier();
        var       clientStore                  = new SqliteClientStore(options.databaseConnectionString()).initialize();

        onWebmentionReceived.subscribe(new WebmentionReceivedSubscriber<>((WebmentionRepository) webmentionRepository, webmentionNotifier));
        webmentionProcessor.start();

        var defaultJwtManager = new DefaultTokenManager(Algorithm.HMAC256(options.oauthJwtSecret()), options.oauthIssuer(), options.oauthAccessTokenValiditySeconds());

        var javalin = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(new ObjectMapper().registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false), true));
            config.registerPlugin(new OAuthServerPlugin("/oauth/token", options.oauthAccessTokenValiditySeconds(), clientStore, defaultJwtManager));
            config.registerPlugin(new OAuthResourceServerPlugin(defaultJwtManager));
            if (options.isOauthManagementApiEnabled()) {
                config.registerPlugin(new OAuthManagementPlugin(clientStore, true));
            }
            config.registerPlugin(new WebmentionPlugin(options.webmentionEndpoint(), webmentionProcessor, (WebmentionRepository) webmentionRepository, options.testMode(), options.testPages()));
        });

        javalin.start(options.httpServerPort());
    }
}
