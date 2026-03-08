package no.clueless.webmention.core.receiver;

import no.clueless.webmention.core.UnexpectedContentTypeException;
import no.clueless.webmention.core.WebmentionEndpointDiscoverer;
import no.clueless.webmention.core.WebmentionException;
import no.clueless.webmention.core.http.SecureHttpClient;
import no.clueless.webmention.core.http.WebmentionHttpRequestBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Set;

/**
 * A default implementation of {@link WebmentionTargetVerifier}.
 */
public class DefaultWebmentionTargetVerifier implements WebmentionTargetVerifier {
    @NotNull
    private final Set<String>                  supportedDomains;
    @NotNull
    private final SecureHttpClient             httpClient;
    @NotNull
    private final WebmentionEndpointDiscoverer discoverer;

    public DefaultWebmentionTargetVerifier(@NotNull Set<String> supportedDomains, @NotNull SecureHttpClient httpClient, @NotNull WebmentionEndpointDiscoverer discoverer) {
        if (supportedDomains.isEmpty()) {
            throw new IllegalArgumentException("supportedDomains cannot be empty");
        }
        this.supportedDomains = supportedDomains;
        this.httpClient       = httpClient;
        this.discoverer       = discoverer;
    }

    @NotNull
    private HttpResponse<String> fetch(@NotNull URI uri) throws WebmentionException {
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
    public boolean verify(@NotNull URI targetUri) throws WebmentionException {
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
