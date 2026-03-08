package no.clueless.webmention.core.sender;

import no.clueless.webmention.core.UnexpectedContentTypeException;
import no.clueless.webmention.core.UnexpectedStatusCodeException;
import no.clueless.webmention.core.WebmentionEndpointDiscoverer;
import no.clueless.webmention.core.WebmentionException;
import no.clueless.webmention.core.http.SecureHttpClient;
import no.clueless.webmention.core.http.WebmentionHttpRequestBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.Collectors;

/**
 * A no.clueless.webmention.core.receiver.Webmention Sender implementation.
 */
public class WebmentionSender {
    private static final Logger                               log = LoggerFactory.getLogger(WebmentionSender.class);
    @NotNull
    private final        SecureHttpClient                     httpClient;
    @Nullable
    private final        SubmissionPublisher<HttpResponse<?>> onReceiverNotifiedPublisher;
    @NotNull
    private final        WebmentionEndpointDiscoverer         webmentionEndpointDiscoverer;

    public WebmentionSender(@NotNull SecureHttpClient httpClient, @Nullable SubmissionPublisher<HttpResponse<?>> onReceiverNotifiedPublisher, @NotNull WebmentionEndpointDiscoverer webmentionEndpointDiscoverer) {
        this.httpClient                   = httpClient;
        this.onReceiverNotifiedPublisher  = onReceiverNotifiedPublisher;
        this.webmentionEndpointDiscoverer = webmentionEndpointDiscoverer;
    }

    void notifyReceiver(@NotNull String webmentionEndpoint, @NotNull String sourceUrl, @NotNull String targetUrl) {
        var formData    = Map.of("sourceUrl", sourceUrl, "targetUrl", targetUrl);
        var encodedForm = formData.entrySet().stream().map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        var httpRequest = WebmentionHttpRequestBuilder.newBuilder()
                .uri(URI.create(webmentionEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodedForm))
                .build();

        HttpResponse<String> postResponse;
        try {
            postResponse = httpClient.send(httpRequest);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP request to webmentionEndpoint " + webmentionEndpoint + " failed", e);
        }

        if (postResponse.statusCode() < 200 || postResponse.statusCode() > 299) {
            throw new UnexpectedStatusCodeException(webmentionEndpoint, postResponse.statusCode());
        }

        if (postResponse.statusCode() == 201) {
            var location = postResponse.headers().firstValue("Location").orElse(null);
            log.debug("Location: {}", location);
        }

        if (onReceiverNotifiedPublisher != null) {
            onReceiverNotifiedPublisher.submit(postResponse);
        }
    }

    public void send(String sourceUrl, String targetUrl) throws WebmentionEndpointNotFoundException, WebmentionException, UnexpectedContentTypeException {
        var webmentionEndpoint = webmentionEndpointDiscoverer.discover(URI.create(targetUrl)).orElseThrow(() -> new WebmentionEndpointNotFoundException(targetUrl));
        notifyReceiver(webmentionEndpoint, sourceUrl, targetUrl);
    }

    public static class Builder {
        @Nullable
        private SecureHttpClient                     httpClient;
        @Nullable
        private SubmissionPublisher<HttpResponse<?>> submissionPublisher;
        @Nullable
        private WebmentionEndpointDiscoverer         endpointDiscoverer;

        private Builder() {
        }

        @NotNull
        public Builder httpClient(@Nullable SecureHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        @NotNull
        public Builder submissionPublisher(@Nullable SubmissionPublisher<HttpResponse<?>> submissionPublisher) {
            this.submissionPublisher = submissionPublisher;
            return this;
        }

        @NotNull
        public Builder endpointDiscoverer(@Nullable WebmentionEndpointDiscoverer endpointDiscoverer) {
            this.endpointDiscoverer = endpointDiscoverer;
            return this;
        }

        @NotNull
        public WebmentionSender build() {
            if (httpClient == null) {
                throw new IllegalStateException("httpClient cannot be null");
            }
            if (endpointDiscoverer == null) {
                throw new IllegalStateException("endpointDiscoverer cannot be null");
            }
            return new WebmentionSender(httpClient, submissionPublisher, endpointDiscoverer);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
}