package no.clueless.webmention;

import no.clueless.webmention.http.SecureHttpClient;
import no.clueless.webmention.http.WebmentionHttpRequestBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Pattern;

public class WebmentionEndpointDiscoverer {
    private static final Logger           log = LoggerFactory.getLogger(WebmentionEndpointDiscoverer.class);
    private final        SecureHttpClient httpClient;

    public WebmentionEndpointDiscoverer(SecureHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    }

    Document parseHtml(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("html cannot be null or blank");
        }
        return Jsoup.parse(html);
    }

    protected Optional<String> findInHeaders(HttpHeaders httpHeaders) {
        return Objects.requireNonNull(httpHeaders, "httpHeaders cannot be null")
                .map()
                .entrySet()
                .stream()
                .filter(entry -> "link".equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .flatMap(header -> Arrays.stream(header.split(",")).map(String::trim))
                .filter(header -> {
                    var pattern = Pattern.compile("rel=\"?([^\";]+)\"?$");
                    var matcher = pattern.matcher(header);

                    if (matcher.find()) {
                        var value  = matcher.group(1);
                        var tokens = Arrays.asList(value.split("\\s+"));
                        return tokens.contains("webmention");
                    } else {
                        log.debug("No webmention match was found in Link header: {}", header);
                    }

                    return false;
                })
                .findFirst()
                .map(header -> header.substring(header.indexOf("<") + 1, header.indexOf(">")));
    }

    protected Optional<String> findInHtml(Document document) {
        return Objects.requireNonNull(document, "document cannot be null")
                .select("a[rel*=webmention], link[rel*=webmention]")
                .stream()
                .filter(link -> {
                    var rel    = link.attr("rel").toLowerCase();
                    var tokens = Arrays.asList(rel.split("\\s+"));
                    return tokens.contains("webmention");
                })
                .map(link -> link.attr("href"))
                .findFirst();
    }

    public Optional<String> discover(URI targetUri, HttpResponse<String> httpResponse) throws UnexpectedContentTypeException {
        Objects.requireNonNull(targetUri, "targetUri cannot be null");
        Objects.requireNonNull(httpResponse, "httpResponse cannot be null");

        var contentType = httpResponse.headers()
                .firstValue("Content-Type")
                .orElseGet(() -> httpResponse.headers()
                        .firstValue("content-type")
                        .orElse(null)
                );

        if (contentType == null) {
            throw new UnexpectedContentTypeException("HTTP response from targetUrl URL " + targetUri + " did not contain a Content-Type header");
        }

        if (!contentType.startsWith("text/html")) {
            throw new UnexpectedContentTypeException(targetUri.toString(), contentType);
        }

        return findInHeaders(httpResponse.headers())
                .or(() -> {
                    var contentLength = httpResponse.headers()
                            .firstValue("Content-Length")
                            .map(Long::parseLong)
                            .orElseGet(() -> httpResponse.headers()
                                    .firstValue("content-length")
                                    .map(Long::parseLong)
                                    .orElse(null)
                            );
                    if (contentLength != null && contentLength == 0) {
                        log.info("HTTP response from {} returned Content-Length = 0", targetUri);
                        return Optional.empty();
                    }

                    if (httpResponse.body().isBlank()) {
                        log.info("HTTP response from {} contained an empty body", targetUri);
                        return Optional.empty();
                    }

                    var document = parseHtml(httpResponse.body());
                    return findInHtml(document);
                })
                .map(webmentionEndpoint -> {
                    if (webmentionEndpoint.isBlank()) {
                        return targetUri.toString();
                    }

                    if (URI.create(webmentionEndpoint).isAbsolute()) {
                        return webmentionEndpoint;
                    }

                    return webmentionEndpoint.startsWith("/") ? targetUri.getScheme() + "://" + targetUri.getAuthority() + webmentionEndpoint : targetUri + "/" + webmentionEndpoint;
                });
    }

    public Optional<String> discover(URI targetUri) throws UnexpectedContentTypeException {
        Objects.requireNonNull(targetUri, "targetUri cannot be null");

        var httpRequest = WebmentionHttpRequestBuilder.newBuilder()
                .uri(targetUri)
                .GET()
                .build();

        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(httpRequest);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP request to targetUrl URL " + targetUri + " failed", e);
        }

        return discover(targetUri, httpResponse);
    }

    public static class Builder {
        private SecureHttpClient httpClient;

        private Builder() {
        }

        public Builder httpClient(SecureHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public WebmentionEndpointDiscoverer build() {
            return new WebmentionEndpointDiscoverer(httpClient);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
}
