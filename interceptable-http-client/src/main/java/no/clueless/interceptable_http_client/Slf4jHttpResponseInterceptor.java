package no.clueless.interceptable_http_client;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;

public class Slf4jHttpResponseInterceptor implements HttpResponseInterceptor {
    private static final Logger log = LoggerFactory.getLogger(Slf4jHttpResponseInterceptor.class);

    @Override
    public void intercept(@NotNull HttpResponse<?> response) {
        if(log.isDebugEnabled()) {
            log.debug("Received response: {}", response);
        }
    }
}
