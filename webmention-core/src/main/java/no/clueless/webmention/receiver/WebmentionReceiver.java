package no.clueless.webmention.receiver;

import no.clueless.webmention.ContentLengthExceededException;
import no.clueless.webmention.UnexpectedStatusCodeException;
import no.clueless.webmention.WebmentionException;
import no.clueless.webmention.event.WebmentionEvent;
import no.clueless.webmention.http.SecureHttpClient;
import no.clueless.webmention.http.WebmentionHttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.SubmissionPublisher;

/**
 * A no.clueless.webmention.receiver.Webmention Receiver implementation.
 */
public class WebmentionReceiver {
    private static final Logger                               log = LoggerFactory.getLogger(WebmentionReceiver.class);
    private final        SecureHttpClient                     httpClient;
    private final        WebmentionRequestVerifier            webmentionRequestVerifier;
    private final        SubmissionPublisher<WebmentionEvent> onWebmentionReceived;

    public WebmentionReceiver(SecureHttpClient httpClient, WebmentionRequestVerifier webmentionRequestVerifier, SubmissionPublisher<WebmentionEvent> onWebmentionReceived) {
        this.httpClient                = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.webmentionRequestVerifier = Objects.requireNonNull(webmentionRequestVerifier, "requestVerifier cannot be null");
        this.onWebmentionReceived      = Objects.requireNonNull(onWebmentionReceived, "onWebmentionReceived cannot be null");
    }

    public void receive(String sourceUrl, String targetUrl) throws WebmentionException {
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
        private SecureHttpClient                     secureHttpClient;
        private WebmentionRequestVerifier            requestVerifier;
        private SubmissionPublisher<WebmentionEvent> onWebmentionReceived;

        private Builder() {
        }

        public Builder httpClient(SecureHttpClient httpClient) {
            this.secureHttpClient = httpClient;
            return this;
        }

        public Builder requestVerifier(WebmentionRequestVerifier requestVerifier) {
            this.requestVerifier = requestVerifier;
            return this;
        }

        public Builder onWebmentionReceived(SubmissionPublisher<WebmentionEvent> onWebmentionReceived) {
            this.onWebmentionReceived = onWebmentionReceived;
            return this;
        }

        public WebmentionReceiver build() {
            return new WebmentionReceiver(secureHttpClient, requestVerifier, onWebmentionReceived);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
}
