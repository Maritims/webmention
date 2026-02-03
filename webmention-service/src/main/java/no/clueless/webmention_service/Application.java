package no.clueless.webmention_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import no.clueless.webmention.WebmentionEndpointDiscoverer;
import no.clueless.webmention.event.WebmentionEvent;
import no.clueless.webmention.event.WebmentionReceivedSubscriber;
import no.clueless.webmention.http.SecureHttpClient;
import no.clueless.webmention.notifier.email.WebmentionEmailViaResendNotifier;
import no.clueless.webmention.persistence.sqlite.SqliteWebmentionRepository;
import no.clueless.webmention.receiver.*;
import no.clueless.webmention_javalin.WebmentionPlugin;
import no.clueless.webmention.sender.WebmentionSender;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.SubmissionPublisher;

public class Application {
    public static void main(String[] args) {
        final var serverPort         = Optional.ofNullable(System.getenv("WEBMENTION_SERVER_PORT")).filter(property -> !property.isBlank()).map(Integer::parseInt).orElse(8080);
        final var connectionString   = Optional.ofNullable(System.getenv("WEBMENTION_DB_CONNECTION_STRING")).filter(property -> !property.isBlank()).orElse("jdbc:sqlite:webmentions.db");
        final var webmentionEndpoint = Optional.ofNullable(System.getenv("WEBMENTION_ENDPOINT")).orElse("/webmention");
        final var supportedDomains   = Optional.ofNullable(System.getenv("WEBMENTION_SUPPORTED_DOMAINS")).map(value -> new HashSet<>(Arrays.asList(value.split(",")))).orElseThrow(() -> new IllegalStateException("WEBMENTION_SUPPORTED_DOMAINS must be set"));
        final var testMode           = Optional.ofNullable(System.getenv("WEBMENTION_TEST_MODE")).map("true"::equalsIgnoreCase).orElse(false);
        final var connectTimeout     = Optional.ofNullable(System.getenv("WEBMENTION_CONNECTION_TIMEOUT_IN_MILLISECONDS")).map(Long::parseLong).map(Duration::ofMillis).orElse(Duration.ofMillis(5000));

        final var httpClient = SecureHttpClient.newClient(connectTimeout, !testMode);
        final var webmentionEndpointDiscoverer = WebmentionEndpointDiscoverer.newBuilder()
                .httpClient(httpClient)
                .build();
        final var targetVerifier = DefaultWebmentionTargetVerifier.newBuilder()
                .supportedDomains(supportedDomains)
                .httpClient(httpClient)
                .endpointDiscoverer(webmentionEndpointDiscoverer)
                .build();
        final var webmentionRepository = new SqliteWebmentionRepository(connectionString).initialize();
        final var onWebmentionReceived = new SubmissionPublisher<WebmentionEvent>();
        final var receiver = WebmentionReceiver.newBuilder()
                .httpClient(httpClient)
                .requestVerifier(WebmentionRequestVerifier.newBuilder().targetVerifier(targetVerifier).build())
                .onWebmentionReceived(onWebmentionReceived)
                .build();
        final var webmentionProcessor = WebmentionProcessor.newBuilder()
                .rateLimiter(WebmentionRateLimiter.newBuilder().maxEntries(5000).cooldownMillis(5).build())
                .receiver(receiver)
                .build();
        final var webmentionSender = WebmentionSender.newBuilder()
                .httpClient(httpClient)
                .submissionPublisher(new SubmissionPublisher<>())
                .endpointDiscoverer(webmentionEndpointDiscoverer)
                .build();
        final var webmentionNotifier = new WebmentionEmailViaResendNotifier();

        onWebmentionReceived.subscribe(new WebmentionReceivedSubscriber<>(webmentionRepository, webmentionNotifier));
        webmentionProcessor.start();

        var javalin = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false), true
            ));

            config.registerPlugin(new WebmentionPlugin(plugin -> {
                plugin.setEndpoint(webmentionEndpoint);
                plugin.setProcessor(webmentionProcessor);
                plugin.setSender(webmentionSender);
                plugin.setWebmentionRepository(webmentionRepository);
                plugin.setTestMode(testMode);
            }));
        });

        javalin.start(serverPort);
    }
}
