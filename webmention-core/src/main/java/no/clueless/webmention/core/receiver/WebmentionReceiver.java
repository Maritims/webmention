package no.clueless.webmention.core.receiver;

import no.clueless.webmention.core.ContentLengthExceededException;
import no.clueless.webmention.core.UnexpectedStatusCodeException;
import no.clueless.webmention.core.WebmentionException;
import no.clueless.webmention.core.event.WebmentionEvent;
import no.clueless.webmention.core.http.SecureHttpClient;
import no.clueless.webmention.core.http.WebmentionHttpRequestBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.concurrent.SubmissionPublisher;

/**
 * A no.clueless.webmention.core.receiver.Webmention Receiver implementation.
 */
public class WebmentionReceiver {
    private static final Logger                               log = LoggerFactory.getLogger(WebmentionReceiver.class);
    @NotNull
    private final        SecureHttpClient                     httpClient;
    @NotNull
    private final        WebmentionRequestVerifier            webmentionRequestVerifier;
    @NotNull
    private final        SubmissionPublisher<WebmentionEvent> onWebmentionReceived;

    public WebmentionReceiver(@NotNull SecureHttpClient httpClient, @NotNull WebmentionRequestVerifier webmentionRequestVerifier, @NotNull SubmissionPublisher<WebmentionEvent> onWebmentionReceived) {
        this.httpClient                = httpClient;
        this.webmentionRequestVerifier = webmentionRequestVerifier;
        this.onWebmentionReceived      = onWebmentionReceived;
    }

    public void receive(@NotNull String sourceUrl, @NotNull String targetUrl) throws WebmentionException {
        if (!webmentionRequestVerifier.verify(sourceUrl, targetUrl)) {
            throw new WebmentionException("Request from sourceUrl " + sourceUrl + " to targetUrl " + targetUrl + " did not pass verification");
        }

        HttpResponse<String> httpResponse;
        try {
            var httpRequest = WebmentionHttpRequestBuilder.newBuilder().uri(URI.create(sourceUrl)).GET().build();
            httpResponse = httpClient.send(httpRequest);
        } catch (ContentLengthExceededException e) {
            log.warn("Webmention fetch blocked: {} - {}", sourceUrl, e.getMessage());
            return;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP request to sourceUrl " + sourceUrl + " failed", e);
        }

        if (httpResponse.statusCode() != 200) {
            throw new UnexpectedStatusCodeException(sourceUrl, httpResponse.statusCode());
        }

        var contentType = httpResponse.headers()
                .map()
                .entrySet()
                .stream()
                .filter(entry -> "content-type".equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("HTTP request to sourceUrl " + sourceUrl + " did not return a Content-Type header"));

        var sourceScanner = WebmentionSourceScanner.resolve(contentType);
        var mentionText   = sourceScanner.findTargetUrlMention(httpResponse.body(), targetUrl).orElseThrow(() -> new WebmentionException("The targetUrl URL " + targetUrl + " is not mentioned in the document at sourceUrl URL " + sourceUrl));

        onWebmentionReceived.submit(new WebmentionEvent(sourceUrl, targetUrl, mentionText));
    }

    public static class Builder {
        @Nullable
        private SecureHttpClient                     secureHttpClient;
        @Nullable
        private WebmentionRequestVerifier            requestVerifier;
        @Nullable
        private SubmissionPublisher<WebmentionEvent> onWebmentionReceived;

        private Builder() {
        }

        @NotNull
        public Builder httpClient(@Nullable SecureHttpClient httpClient) {
            this.secureHttpClient = httpClient;
            return this;
        }

        @NotNull
        public Builder requestVerifier(@Nullable WebmentionRequestVerifier requestVerifier) {
            this.requestVerifier = requestVerifier;
            return this;
        }

        public Builder onWebmentionReceived(@Nullable SubmissionPublisher<WebmentionEvent> onWebmentionReceived) {
            this.onWebmentionReceived = onWebmentionReceived;
            return this;
        }

        @NotNull
        public WebmentionReceiver build() {
            if (secureHttpClient == null) {
                throw new IllegalStateException("secureHttpClient cannot be null");
            }
            if (requestVerifier == null) {
                throw new IllegalStateException("requestVerifier cannot be null");
            }
            if (onWebmentionReceived == null) {
                throw new IllegalStateException("onWebmentionReceived cannot be null");
            }
            return new WebmentionReceiver(secureHttpClient, requestVerifier, onWebmentionReceived);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
}
