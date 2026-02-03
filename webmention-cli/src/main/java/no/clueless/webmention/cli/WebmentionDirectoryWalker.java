package no.clueless.webmention.cli;

import no.clueless.webmention.event.WebmentionEvent;
import no.clueless.webmention.receiver.WebmentionHtmlSourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebmentionDirectoryWalker {
    private static final Logger log = LoggerFactory.getLogger(WebmentionDirectoryWalker.class);

    private final WebmentionHtmlSourceScanner webmentionHtmlSourceScanner;
    private final Set<String>                 supportedFileExtensions;

    public WebmentionDirectoryWalker(WebmentionHtmlSourceScanner webmentionHtmlSourceScanner, Set<String> supportedFileExtensions) {
        this.webmentionHtmlSourceScanner = Objects.requireNonNull(webmentionHtmlSourceScanner, "webmentionHtmlSourceScanner");
        this.supportedFileExtensions     = Objects.requireNonNull(supportedFileExtensions, "supportedFileExtensions cannot be null");

        if (supportedFileExtensions.isEmpty()) {
            throw new IllegalArgumentException("supportedFileExtensions cannot be empty");
        }
    }

    String createSourceUrl(URI baseUri, Path rootDir, Path file) {
        Objects.requireNonNull(baseUri, "baseUri cannot be null");
        Objects.requireNonNull(rootDir, "rootDir cannot be null");
        Objects.requireNonNull(file, "file cannot be null");

        // Relativise the file: /var/www/site/foo/bar.html -> foo/bar.html
        var relative = rootDir.relativize(file);

        // Resolve against base: https://clueless.no/ + foo/bar.html
        // Ensure any backslashes (from Windows, for example) are replaced by forward slashes like in URLs.
        var base = baseUri.toString();
        if (!base.endsWith("/")) {
            base += "/";
        }
        var pathFragment = relative.toString().replace(file.getFileSystem().getSeparator(), "/");
        return URI.create(base).resolve(pathFragment).toString();
    }

    public Set<WebmentionEvent> walk(URI baseUri, Path rootDir, Set<WebmentionEvent> webmentionEvents) throws IOException {
        Objects.requireNonNull(rootDir, "rootDir cannot be null");
        Objects.requireNonNull(webmentionEvents, "webmentionEvents cannot be null");

        if (!Files.exists(rootDir)) {
            throw new FileNotFoundException("Path does not exist: " + rootDir);
        }

        if (!Files.isDirectory(rootDir)) {
            throw new NotDirectoryException(rootDir.toString());
        }

        try (var stream = Files.walk(rootDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(file -> supportedFileExtensions.stream().anyMatch(fileExtension -> file.getFileName().toString().endsWith("." + fileExtension)))
                    .flatMap(file -> {
                        try {
                            var body      = Files.readString(file);
                            var sourceUrl = createSourceUrl(baseUri, rootDir, file);
                            return webmentionHtmlSourceScanner.findAllMentions(body)
                                    .entrySet()
                                    .stream()
                                    .map(entry -> new WebmentionEvent(sourceUrl, entry.getKey().toString(), entry.getValue()));
                        } catch (IOException e) {
                            log.error("An exception occurred while processing file: {}", file, e);
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toSet());
        }
    }
}
