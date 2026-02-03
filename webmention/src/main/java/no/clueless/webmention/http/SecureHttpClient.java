package no.clueless.webmention.http;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class SecureHttpClient {
    private final HttpClient httpClient;
    private final long       maxContentLengthInBytes;

    public SecureHttpClient(HttpClient httpClient, long maxContentLengthInBytes) {
        if (maxContentLengthInBytes < 1) {
            throw new IllegalArgumentException("maxContentLengthInBytes must be greater than zero");
        }
        this.httpClient              = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.maxContentLengthInBytes = maxContentLengthInBytes;
    }

    public HttpResponse<String> send(HttpRequest httpRequest) throws IOException, InterruptedException {
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

    private static boolean isRestricted(URI uri) {
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

    public static SecureHttpClient newClient(Duration connectTimeout, boolean blockRestricted) {
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