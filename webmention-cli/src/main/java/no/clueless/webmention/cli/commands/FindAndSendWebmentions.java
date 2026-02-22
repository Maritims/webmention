package no.clueless.webmention.cli.commands;

import no.clueless.webmention.UnexpectedContentTypeException;
import no.clueless.webmention.WebmentionEndpointDiscoverer;
import no.clueless.webmention.WebmentionException;
import no.clueless.webmention.cli.*;
import no.clueless.webmention.event.WebmentionEvent;
import no.clueless.webmention.http.SecureHttpClient;
import no.clueless.webmention.receiver.WebmentionHtmlSourceScanner;
import no.clueless.webmention.sender.WebmentionEndpointNotFoundException;
import no.clueless.webmention.sender.WebmentionSender;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Set;

/**
 * Find and send webmentions from a given directory.
 */
public class FindAndSendWebmentions implements Command {
    private static final Logger log = LoggerFactory.getLogger(FindAndSendWebmentions.class);

    /**
     * Default constructor.
     */
    public FindAndSendWebmentions() {
    }

    @NotNull
    Set<WebmentionEvent> findWebmentionEvents(@NotNull URI baseUri, @NotNull Path rootDir, boolean restrictive) {
        if (!rootDir.toFile().exists()) {
            throw new IllegalArgumentException("rootDir does not exist");
        }
        if (!rootDir.toFile().isDirectory()) {
            throw new IllegalArgumentException("rootDir is not a directory");
        }

        var webmentionDirectoryWalker = new WebmentionDirectoryWalker(new WebmentionHtmlSourceScanner(), Set.of("htm", "html"));
        try {
            return webmentionDirectoryWalker.walk(baseUri, rootDir, (element) -> !restrictive || element.hasAttr("data-webmention"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void sendWebmention(@NotNull WebmentionEvent webmentionEvent, boolean dryRun, @NotNull WebmentionSender webmentionSender) {
        if (dryRun) {
            log.info("[Dry Run] Webmention would have been sent: {} -> {}", webmentionEvent.sourceUrl(), webmentionEvent.targetUrl());
            return;
        }

        try {
            webmentionSender.send(webmentionEvent.sourceUrl(), webmentionEvent.targetUrl());
            log.info("Webmention was sent: {} -> {}", webmentionEvent.sourceUrl(), webmentionEvent.targetUrl());
        } catch (WebmentionEndpointNotFoundException | UnexpectedContentTypeException e) {
            log.warn("Webmention was not sent: {} -> {}: {}", webmentionEvent.sourceUrl(), webmentionEvent.targetUrl(), e.getMessage());
        } catch (WebmentionException e) {
            log.warn("Webmention was not sent: {} -> {}", webmentionEvent.sourceUrl(), webmentionEvent.targetUrl(), e);
        }
    }

    void sendWebmentions(@NotNull Set<WebmentionEvent> webmentionEvents, boolean dryRun, @NotNull WebmentionSender webmentionSender) {
        webmentionEvents.forEach(webmentionEvent -> sendWebmention(webmentionEvent, dryRun, webmentionSender));
    }

    @NotNull
    public CommandResult execute(@NotNull String[] args) {
        String uriStr      = null;
        String dirStr      = null;
        var    dryRun      = false;
        var    restrictive = false;

        for (var i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--uri", "-u" -> {
                    if (++i < args.length) {
                        uriStr = args[i];
                        log.debug("Setting uri to: {}", uriStr);
                    }
                }
                case "--dir", "-d" -> {
                    if (++i < args.length) {
                        dirStr = args[i];
                        log.debug("Setting dir to: {}", dirStr);
                    }
                }
                case "--dry-run", "-dr" -> {
                    dryRun = true;
                    log.debug("Dry run enabled");
                }
                case "--restrictive", "-r" -> {
                    restrictive = true;
                    log.debug("Restrictive mode enabled");
                }
                default -> {
                    return CommandResult.UNKNOWN_ARGUMENT;
                }
            }
        }

        if (uriStr == null || dirStr == null) {
            log.error("Both --uri and --dir are required");
            return CommandResult.MISSING_REQUIRED_ARGUMENT;
        }
        try {
            var rootDir          = Path.of(dirStr);
            var baseUri          = new URI(uriStr);
            var httpClient       = new SecureHttpClient(HttpClient.newBuilder().build(), 1024 * 1024 * 1024);
            var webmentionSender = new WebmentionSender(new SecureHttpClient(HttpClient.newBuilder().build(), 1024 * 1024 * 1024), null, new WebmentionEndpointDiscoverer(httpClient));
            var webmentionEvents = findWebmentionEvents(baseUri, rootDir, restrictive);
            sendWebmentions(webmentionEvents, dryRun, webmentionSender);
        } catch (URISyntaxException e) {
            log.error("Invalid URI: {}", uriStr);
            return CommandResult.INVALID_ARGUMENT;
        }

        return CommandResult.SUCCESS;
    }
}
