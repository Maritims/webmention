package no.clueless.webmention.http;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * A secure HTTP client.
 */
public class SecureHttpClient {
    @NotNull
    private final HttpClient httpClient;
    private final long       maxContentLengthInBytes;

    /**
     * Constructor.
     *
     * @param httpClient              the underlying HTTP client.
     * @param maxContentLengthInBytes the maximum content length in bytes of a response.
     */
    public SecureHttpClient(@NotNull HttpClient httpClient, long maxContentLengthInBytes) {
        if (maxContentLengthInBytes < 1) {
            throw new IllegalArgumentException("maxContentLengthInBytes must be greater than zero");
        }
        this.httpClient              = httpClient;
        this.maxContentLengthInBytes = maxContentLengthInBytes;
    }

    /**
     * Sends the given HTTP request.
     *
     * @param httpRequest the request to send
     * @return the response
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the thread is interrupted
     */
    @NotNull
    public HttpResponse<String> send(@NotNull HttpRequest httpRequest) throws IOException, InterruptedException {
        Objects.requireNonNull(httpRequest);

        return httpClient.send(httpRequest, responseInfo -> {
            if (responseInfo.statusCode() >= 200 && responseInfo.statusCode() <= 299) {
                var contentLength = responseInfo.headers().firstValueAsLong("Content-Length").orElse(0L);
                if (contentLength > maxContentLengthInBytes * 1024) {
                    return HttpResponse.BodySubscribers.replacing("File too large");
                }
            }

            // Never trust the Content-Length to not lie about the actual length.
            var stringSubscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
            return new LimitedBodySubscriber<>(stringSubscriber, maxContentLengthInBytes);
        });
    }

    /**
     * Determines whether the given URI is restricted.
     *
     * @param uri the URI to check
     * @return true if the URI is restricted, false otherwise
     */
    private static boolean isRestricted(@NotNull URI uri) {
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            return false;
        }

        InetAddress address;
        try {
            address = InetAddress.getByName(uri.getHost());
        } catch (UnknownHostException e) {
            return true;
        }

        return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
    }

    /**
     * Creates a new secure HTTP client.
     *
     * @param connectTimeout  the connect timeout
     * @param blockRestricted whether to block restricted URIs
     * @return the client
     */
    @NotNull
    public static SecureHttpClient newClient(@NotNull Duration connectTimeout, boolean blockRestricted) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_2)
                .proxy(new ProxySelector() {
                    @Override
                    public List<Proxy> select(URI uri) {
                        if (blockRestricted && isRestricted(uri)) {
                            throw new UnsupportedOperationException("Loopback addresses are blocked");
                        }
                        return List.of(Proxy.NO_PROXY);
                    }

                    @Override
                    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    }
                })
                .build();
        return new SecureHttpClient(httpClient, 1024 * 1024 * 1024);
    }
}