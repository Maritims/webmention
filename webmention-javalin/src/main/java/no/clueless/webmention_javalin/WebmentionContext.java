package no.clueless.webmention_javalin;

import io.javalin.http.Context;
import no.clueless.webmention.UnexpectedContentTypeException;
import no.clueless.webmention.WebmentionException;
import no.clueless.webmention.sender.WebmentionEndpointNotFoundException;

import java.util.Objects;

public record WebmentionContext(Context ctx, WebmentionConfig config) {
    public WebmentionContext {
        Objects.requireNonNull(ctx, "ctx cannot be null");
        Objects.requireNonNull(config, "config cannot be null");
    }

    public void send(String source, String target) throws WebmentionEndpointNotFoundException, WebmentionException, UnexpectedContentTypeException {
        Objects.requireNonNull(source, "sourceUrl cannot be null");
        Objects.requireNonNull(target, "targetUrl cannot be null");
        config.getSender().send(source, target);
    }
}