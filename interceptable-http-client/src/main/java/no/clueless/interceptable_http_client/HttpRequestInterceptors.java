package no.clueless.interceptable_http_client;

import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.*;

public class HttpRequestInterceptors implements Iterable<HttpRequestInterceptor> {
    @NotNull
    private final LinkedHashSet<HttpRequestInterceptor> httpRequestInterceptors;

    public HttpRequestInterceptors() {
        this.httpRequestInterceptors = new LinkedHashSet<>();
    }

    @NotNull
    public HttpRequestInterceptors add(@NotNull HttpRequestInterceptor httpRequestInterceptor) {
        httpRequestInterceptors.add(httpRequestInterceptor);
        return this;
    }

    public HttpRequest intercept(@NotNull HttpRequest request, @NotNull HttpClient httpClient) {
        for(var interceptor : httpRequestInterceptors) {
            request = interceptor.intercept(request, httpClient);
        }
        return request;
    }

    @Override
    public @NotNull Iterator<HttpRequestInterceptor> iterator() {
        return httpRequestInterceptors.iterator();
    }
}
