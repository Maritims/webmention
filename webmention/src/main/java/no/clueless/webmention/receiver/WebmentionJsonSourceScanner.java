package no.clueless.webmention.receiver;

import java.util.Optional;

public class WebmentionJsonSourceScanner implements WebmentionSourceScanner {
    @Override
    public Optional<String> findTargetUrlMention(String body, String targetUrl) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
