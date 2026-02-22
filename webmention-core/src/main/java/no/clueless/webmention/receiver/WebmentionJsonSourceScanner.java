package no.clueless.webmention.receiver;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class WebmentionJsonSourceScanner implements WebmentionSourceScanner {
    @Override
    public @NotNull Optional<String> findTargetUrlMention(@NotNull String body, @NotNull String targetUrl) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
