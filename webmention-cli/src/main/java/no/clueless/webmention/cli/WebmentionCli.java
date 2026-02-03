package no.clueless.webmention.cli;

import no.clueless.webmention.UnexpectedContentTypeException;
import no.clueless.webmention.WebmentionEndpointDiscoverer;
import no.clueless.webmention.WebmentionException;
import no.clueless.webmention.event.WebmentionEvent;
import no.clueless.webmention.http.SecureHttpClient;
import no.clueless.webmention.receiver.WebmentionHtmlSourceScanner;
import no.clueless.webmention.sender.WebmentionEndpointNotFoundException;
import no.clueless.webmention.sender.WebmentionSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class WebmentionCli {
    private static final Logger                    log = LoggerFactory.getLogger(WebmentionCli.class);
    private final        WebmentionDirectoryWalker webmentionDirectoryWalker;
    private final        WebmentionSender          webmentionSender;

    public WebmentionCli(WebmentionDirectoryWalker webmentionDirectoryWalker, WebmentionSender webmentionSender) {
        this.webmentionDirectoryWalker = Objects.requireNonNull(webmentionDirectoryWalker, "webmentionDirectoryWalker cannot be null");
        this.webmentionSender          = Objects.requireNonNull(webmentionSender, "webmentionSender cannot be null");
    }

    public WebmentionCli() {
        this.webmentionDirectoryWalker = new WebmentionDirectoryWalker(new WebmentionHtmlSourceScanner(), Set.of("htm", "html"));
        var httpClient = new SecureHttpClient(HttpClient.newBuilder().build(), 1024 * 1024 * 1024);
        this.webmentionSender = new WebmentionSender(httpClient, null, new WebmentionEndpointDiscoverer(httpClient));
    }

    public Set<WebmentionEvent> findWebmentionEvents(URI baseUri, Path rootDir) throws IOException {
        Objects.requireNonNull(baseUri, "baseUri cannot be null");
        Objects.requireNonNull(rootDir, "rootDir cannot be null");

        return webmentionDirectoryWalker.walk(baseUri, rootDir, new HashSet<>());
    }

    public void sendWebmention(WebmentionEvent webmentionEvent, boolean dryRun) {
        try {
            if(dryRun) {
                log.info("[Dry Run] Webmention would have been sent: {} -> {}", webmentionEvent.sourceUrl(), webmentionEvent.targetUrl());
            } else {
                webmentionSender.send(webmentionEvent.sourceUrl(), webmentionEvent.targetUrl());
                log.info("Webmention was sent: {} -> {}", webmentionEvent.sourceUrl(), webmentionEvent.targetUrl());
            }
        } catch (WebmentionEndpointNotFoundException | UnexpectedContentTypeException e) {
            log.warn("Webmention was not sent: {} -> {}: {}",  webmentionEvent.sourceUrl(), webmentionEvent.targetUrl(), e.getMessage());
        } catch (WebmentionException e) {
            log.warn("Webmention was not sent: {} -> {}", webmentionEvent.sourceUrl(), webmentionEvent.targetUrl(), e);
        }
    }

    public void sendWebmentions(Set<WebmentionEvent> webmentionEvents, boolean dryRun) {
        webmentionEvents.forEach(webmentionEvent -> {
            try {
                sendWebmention(webmentionEvent, dryRun);
            } catch (Exception e) {
                log.error("An exception occurred while sending webmention: {} -> {}", webmentionEvent.sourceUrl(), webmentionEvent.targetUrl(), e);
            }
        });
    }

    public void findAndSendWebmentions(URI baseUri, Path rootDir, boolean dryRun) throws IOException {
        Objects.requireNonNull(baseUri, "baseUri cannot be null");
        Objects.requireNonNull(rootDir, "rootDir cannot be null");

        var webmentionEvents = findWebmentionEvents(baseUri, rootDir);
        sendWebmentions(webmentionEvents, dryRun);
    }
}
