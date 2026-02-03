package no.clueless.webmention.sender;

import no.clueless.webmention.UnexpectedContentTypeException;
import no.clueless.webmention.UnexpectedStatusCodeException;
import no.clueless.webmention.WebmentionEndpointDiscoverer;
import no.clueless.webmention.WebmentionException;
import no.clueless.webmention.http.SecureHttpClient;
import no.clueless.webmention.http.WebmentionHttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.Collectors;

/**
 * A no.clueless.webmention.receiver.Webmention Sender implementation.
 */
public class WebmentionSender {
    private static final Logger                               log = LoggerFactory.getLogger(WebmentionSender.class);
    private final        SecureHttpClient                     httpClient;
    private final        SubmissionPublisher<HttpResponse<?>> onReceiverNotifiedPublisher;
    private final        WebmentionEndpointDiscoverer         webmentionEndpointDiscoverer;

    public WebmentionSender(SecureHttpClient httpClient, SubmissionPublisher<HttpResponse<?>> onReceiverNotifiedPublisher, WebmentionEndpointDiscoverer webmentionEndpointDiscoverer) {
        this.httpClient                   = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.onReceiverNotifiedPublisher  = onReceiverNotifiedPublisher;
        this.webmentionEndpointDiscoverer = Objects.requireNonNull(webmentionEndpointDiscoverer, "webmentionEndpointDiscoverer cannot be null");
    }

    void notifyReceiver(String webmentionEndpoint, String sourceUrl, String targetUrl) {
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
        private SecureHttpClient                     httpClient;
        private SubmissionPublisher<HttpResponse<?>> submissionPublisher;
        private WebmentionEndpointDiscoverer         endpointDiscoverer;

        private Builder() {
        }

        public Builder httpClient(SecureHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder submissionPublisher(SubmissionPublisher<HttpResponse<?>> submissionPublisher) {
            this.submissionPublisher = submissionPublisher;
            return this;
        }

        public Builder endpointDiscoverer(WebmentionEndpointDiscoverer endpointDiscoverer) {
            this.endpointDiscoverer = endpointDiscoverer;
            return this;
        }

        public WebmentionSender build() {
            return new WebmentionSender(httpClient, submissionPublisher, endpointDiscoverer);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
}