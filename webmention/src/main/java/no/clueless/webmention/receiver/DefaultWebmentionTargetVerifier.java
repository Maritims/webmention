package no.clueless.webmention.receiver;

import no.clueless.webmention.UnexpectedContentTypeException;
import no.clueless.webmention.WebmentionEndpointDiscoverer;
import no.clueless.webmention.WebmentionException;
import no.clueless.webmention.http.SecureHttpClient;
import no.clueless.webmention.http.WebmentionHttpRequestBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Set;

public class DefaultWebmentionTargetVerifier implements WebmentionTargetVerifier {
    private final Set<String>                  supportedDomains;
    private final SecureHttpClient             httpClient;
    private final WebmentionEndpointDiscoverer discoverer;

    public DefaultWebmentionTargetVerifier(Set<String> supportedDomains, SecureHttpClient httpClient, WebmentionEndpointDiscoverer discoverer) {
        this.supportedDomains = Objects.requireNonNull(supportedDomains, "supportedDomains cannot be null");
        if (supportedDomains.isEmpty()) {
            throw new IllegalArgumentException("supportedDomains cannot be empty");
        }
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.discoverer = Objects.requireNonNull(discoverer, "discoverer cannot be null");
    }

    private HttpResponse<String> fetch(URI uri) throws WebmentionException {
        var httpRequest = WebmentionHttpRequestBuilder.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(httpRequest);
        } catch (IOException | InterruptedException e) {
            throw new WebmentionException("HTTP request to URL " + uri + " failed", e);
        }

        if (httpResponse.statusCode() != 200) {
            throw new WebmentionException("URL " + uri + " returned an unexpected status code: " + httpResponse.statusCode());
        }

        return httpResponse;
    }

    @Override
    public boolean verify(URI targetUri) throws WebmentionException {
        if (!supportedDomains.contains(targetUri.getHost())) {
            throw new WebmentionException("Target host " + targetUri.getHost() + " is not supported");
        }

        var httpResponse = fetch(targetUri);
        try {
            discoverer.discover(targetUri, httpResponse).orElseThrow(() -> new WebmentionException("Target URL " + targetUri + " does not accept webmentions"));
        } catch (UnexpectedContentTypeException e) {
            throw new WebmentionException("Unexpected Content-Type from " + targetUri + ": " + e.getContentType(), e);
        }

        return true;
    }

    public static class Builder {
        private Set<String>                  supportedDomains;
        private SecureHttpClient             httpClient;
        private WebmentionEndpointDiscoverer webmentionEndpointDiscoverer;

        private Builder() {
        }

        public Builder supportedDomains(Set<String> supportedDomains) {
            this.supportedDomains = supportedDomains;
            return this;
        }

        public Builder httpClient(SecureHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder endpointDiscoverer(WebmentionEndpointDiscoverer webmentionEndpointDiscoverer) {
            this.webmentionEndpointDiscoverer = webmentionEndpointDiscoverer;
            return this;
        }

        public WebmentionTargetVerifier build() {
            return new DefaultWebmentionTargetVerifier(supportedDomains, httpClient, webmentionEndpointDiscoverer);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
}
