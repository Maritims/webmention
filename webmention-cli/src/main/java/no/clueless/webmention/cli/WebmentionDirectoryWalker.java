package no.clueless.webmention.cli;

import no.clueless.webmention.event.WebmentionEvent;
import no.clueless.webmention.receiver.WebmentionHtmlSourceScanner;
import org.jsoup.nodes.Element;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A recursive directory walker that scans for webmention links in HTML files.
 */
public class WebmentionDirectoryWalker {
    private static final Logger log = LoggerFactory.getLogger(WebmentionDirectoryWalker.class);

    private final WebmentionHtmlSourceScanner webmentionHtmlSourceScanner;
    private final Set<String>                 supportedFileExtensions;

    /**
     * Constructor.
     *
     * @param webmentionHtmlSourceScanner the webmention HTML source scanner.
     * @param supportedFileExtensions     the supported file extensions.
     * @throws NullPointerException if webmentionHtmlSourceScanner or supportedFileExtensions is null or empty.
     */
    public WebmentionDirectoryWalker(WebmentionHtmlSourceScanner webmentionHtmlSourceScanner, Set<String> supportedFileExtensions) {
        this.webmentionHtmlSourceScanner = Objects.requireNonNull(webmentionHtmlSourceScanner, "webmentionHtmlSourceScanner");
        this.supportedFileExtensions     = Objects.requireNonNull(supportedFileExtensions, "supportedFileExtensions cannot be null");

        if (supportedFileExtensions.isEmpty()) {
            throw new IllegalArgumentException("supportedFileExtensions cannot be empty");
        }
    }

    /**
     * Creates a source URL for a given file.
     *
     * @param baseUri the base uri, e.g. <a href="https://clueless.no/">https://clueless.no</a>
     * @param rootDir the root directory, e.g. /var/www/site/
     * @param file    the file, e.g. /var/www/site/foo/bar.html
     * @return the source URL, e.g. <a href="https://clueless.no/foo/bar.html">https://clueless.no/foo/bar.html</a>
     */
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

    /**
     * Walks the given directory recursively and finds webmention links.
     *
     * @param baseUri          The base URI, e.g. https://clueless.no/
     * @param rootDir          The root directory to walk.
     * @param elementFilter    The element filter to use.
     * @param webmentionEvents The webmention events to add found webmentions to.
     * @return The set of found webmentions.
     * @throws IOException If an I/O error occurs.
     */
    public Set<WebmentionEvent> walk(URI baseUri, Path rootDir, Predicate<Element> elementFilter, Set<WebmentionEvent> webmentionEvents) throws IOException {
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
                            return webmentionHtmlSourceScanner.findAllMentions(body, elementFilter)
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
