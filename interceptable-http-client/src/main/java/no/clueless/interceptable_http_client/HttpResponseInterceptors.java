package no.clueless.interceptable_http_client;

import org.jetbrains.annotations.NotNull;

import java.net.http.HttpResponse;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class HttpResponseInterceptors implements Iterable<HttpResponseInterceptor> {
    @NotNull
    private final LinkedHashSet<HttpResponseInterceptor> httpResponseInterceptors = new LinkedHashSet<>();

    @NotNull
    public HttpResponseInterceptors add(@NotNull HttpResponseInterceptor httpResponseInterceptor) {
        httpResponseInterceptors.add(httpResponseInterceptor);
        return this;
    }

    public void intercept(@NotNull HttpResponse<?> httpResponse) {
        for(var interceptor : httpResponseInterceptors) {
            interceptor.intercept(httpResponse);
        }
    }

    @Override
    public @NotNull Iterator<HttpResponseInterceptor> iterator() {
        return httpResponseInterceptors.iterator();
    }
}
