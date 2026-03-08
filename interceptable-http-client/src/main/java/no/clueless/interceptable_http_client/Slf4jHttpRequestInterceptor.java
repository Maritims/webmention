package no.clueless.interceptable_http_client;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public class Slf4jHttpRequestInterceptor implements HttpRequestInterceptor {
    private static final Logger log = LoggerFactory.getLogger(Slf4jHttpRequestInterceptor.class);

    @Override
    public HttpRequest intercept(@NotNull HttpRequest originalRequest, @NotNull HttpClient httpClient) {
        log.debug("Sending request: {}", originalRequest);
        return originalRequest;
    }
}
