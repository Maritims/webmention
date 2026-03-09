package no.clueless.webmention.core.receiver;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class WebmentionTextSourceScanner implements WebmentionSourceScanner {
    @Override
    public @NotNull Optional<String> findTargetUrlMention(@NotNull String body, @NotNull String targetUrl) {
        return body.contains(targetUrl) ? Optional.of(targetUrl) : Optional.empty();
    }
}
