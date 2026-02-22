package no.clueless.webmention.http;

import org.jetbrains.annotations.NotNull;

import java.net.http.HttpRequest;
import java.time.Duration;

public class WebmentionHttpRequestBuilder {
    @NotNull
    public static HttpRequest.Builder newBuilder() {
        return HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "Clueless Webmention Client/1.0 (Java)");
    }
}
