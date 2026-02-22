package no.clueless.webmention.receiver;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@FunctionalInterface
public interface WebmentionSourceScanner {
    @NotNull
    Optional<String> findTargetUrlMention(@NotNull String body, @NotNull String targetUrl);

    @NotNull
    static WebmentionSourceScanner resolve(@NotNull String contentType) {
        if (contentType.startsWith("text/html")) {
            return new WebmentionHtmlSourceScanner();
        }

        return switch (contentType.toLowerCase()) {
            case "text/plain" -> new WebmentionTextSourceScanner();
            case "application/json" -> new WebmentionJsonSourceScanner();
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }
}
