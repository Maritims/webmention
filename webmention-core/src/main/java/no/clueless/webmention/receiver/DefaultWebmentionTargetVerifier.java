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

/**
 * A default implementation of {@link WebmentionTargetVerifier}.
 */
public class DefaultWebmentionTargetVerifier implements WebmentionTargetVerifier {
    private final Set<String>                  supportedDomains;
    private final SecureHttpClient             httpClient;
    private final WebmentionEndpointDiscoverer discoverer;

    /**
     * Constructor.
     *
     * @param supportedDomains the set of supported domains which this verifier will accept webmentions for.
     * @param httpClient       the HTTP client to use for fetching webmention endpoints.
     * @param discoverer       the webmention endpoint discoverer to use for discovering webmention endpoints.
     */
    public DefaultWebmentionTargetVerifier(Set<String> supportedDomains, SecureHttpClient httpClient, WebmentionEndpointDiscoverer discoverer) {
        this.supportedDomains = Objects.requireNonNull(supportedDomains, "supportedDomains cannot be null");
        if (supportedDomains.isEmpty()) {
            throw new IllegalArgumentException("supportedDomains cannot be empty");
        }
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.discoverer = Objects.requireNonNull(discoverer, "discoverer cannot be null");
    }

    /**
     * Fetches the given URI and returns the response.
     *
     * @param uri the URI to fetch
     * @return the response
     * @throws WebmentionException if the fetch failed
     */
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

    /**
     * Verifies that the given URI is a valid webmention target.
     *
     * @param targetUri the URI to verify
     * @return true if the URI is a valid webmention target, false otherwise
     * @throws WebmentionException if the verification failed
     */
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
}
