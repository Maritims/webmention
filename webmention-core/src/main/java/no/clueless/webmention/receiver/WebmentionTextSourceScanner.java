package no.clueless.webmention.receiver;

import java.util.Optional;

public class WebmentionTextSourceScanner implements WebmentionSourceScanner {
    @Override
    public Optional<String> findTargetUrlMention(String body, String targetUrl) {
        return body.contains(targetUrl) ? Optional.of(targetUrl) : Optional.empty();
    }
}
