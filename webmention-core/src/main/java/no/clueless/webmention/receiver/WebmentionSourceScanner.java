package no.clueless.webmention.receiver;

import java.util.Optional;

@FunctionalInterface
public interface WebmentionSourceScanner {
    Optional<String> findTargetUrlMention(String body, String targetUrl);

    static WebmentionSourceScanner resolve(String contentType) {
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
