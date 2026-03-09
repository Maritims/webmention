package no.clueless.interceptable_http_client;

import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public interface HttpRequestInterceptor {
    HttpRequest intercept(@NotNull HttpRequest originalRequest, @NotNull HttpClient httpClient);
}
