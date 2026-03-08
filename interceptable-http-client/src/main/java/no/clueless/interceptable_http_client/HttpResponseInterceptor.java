package no.clueless.interceptable_http_client;

import org.jetbrains.annotations.NotNull;

import java.net.http.HttpResponse;

public interface HttpResponseInterceptor {
    void intercept(@NotNull HttpResponse<?> response);
}
