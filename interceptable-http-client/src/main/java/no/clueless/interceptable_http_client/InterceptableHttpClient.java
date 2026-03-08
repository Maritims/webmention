package no.clueless.interceptable_http_client;

import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class InterceptableHttpClient extends HttpClient {
    @NotNull
    private final HttpClient               delegate;
    @NotNull
    private final HttpRequestInterceptors  httpRequestInterceptors  = new HttpRequestInterceptors();
    @NotNull
    private final HttpResponseInterceptors httpResponseInterceptors = new HttpResponseInterceptors();

    public InterceptableHttpClient(@NotNull HttpClient delegate) {
        this.delegate = delegate;
    }

    @NotNull
    public InterceptableHttpClient addInterceptor(@NotNull HttpRequestInterceptor httpRequestInterceptor) {
        httpRequestInterceptors.add(httpRequestInterceptor);
        return this;
    }

    @NotNull
    public InterceptableHttpClient addInterceptor(@NotNull HttpResponseInterceptor httpResponseInterceptor) {
        httpResponseInterceptors.add(httpResponseInterceptor);
        return this;
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return delegate.cookieHandler();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return delegate.connectTimeout();
    }

    @Override
    public Redirect followRedirects() {
        return delegate.followRedirects();
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return delegate.proxy();
    }

    @Override
    public SSLContext sslContext() {
        return delegate.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
        return delegate.sslParameters();
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return delegate.authenticator();
    }

    @Override
    public Version version() {
        return delegate.version();
    }

    @Override
    public Optional<Executor> executor() {
        return delegate.executor();
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
        request = httpRequestInterceptors.intercept(request, delegate);
        var response = delegate.send(request, responseBodyHandler);
        httpResponseInterceptors.intercept(response);
        return response;
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        request = httpRequestInterceptors.intercept(request, delegate);
        return delegate.sendAsync(request, responseBodyHandler);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        request = httpRequestInterceptors.intercept(request, delegate);
        return delegate.sendAsync(request, responseBodyHandler, pushPromiseHandler);
    }
}
