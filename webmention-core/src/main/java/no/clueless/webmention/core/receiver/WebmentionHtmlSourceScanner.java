package no.clueless.webmention.core.receiver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WebmentionHtmlSourceScanner implements WebmentionSourceScanner {
    private static final Logger log = LoggerFactory.getLogger(WebmentionHtmlSourceScanner.class);

    private String resolveText(Element element) {
        var tagName = element.tagName().toLowerCase();
        return switch (tagName) {
            case "a" -> element.text();
            case "img", "video" -> element.html();
            default -> throw new IllegalStateException("Unexpected value: " + tagName);
        };
    }

    @Override
    public @NotNull Optional<String> findTargetUrlMention(@NotNull String body, @NotNull String targetUrl) {
        var document = Jsoup.parse(body);
        var elements = document.select("a[href=\"" + targetUrl + "\"], img[href=\"" + targetUrl + "\"], video[src=\"" + targetUrl + "\"]");
        return elements.stream()
                .map(this::resolveText)
                .findFirst();
    }

    /**
     * Finds all webmentions in the given body and filters by the given elementFilter.
     *
     * @param body          The HTML body to scan for mentions.
     * @param elementFilter The elementFilter to filter mentions by.
     * @return A map of all mentions found in the body by URI.
     */
    @NotNull
    public Map<URI, String> findAllMentions(@NotNull String body, @Nullable Predicate<Element> elementFilter) {
        var document = Jsoup.parse(body);
        var elements = document.select("a[href], img[href], video[src]");
        return elements.stream()
                .filter(element -> elementFilter == null || elementFilter.test(element))
                .map(element -> {
                    var tagName = element.tagName().toLowerCase();
                    return switch (tagName) {
                        case "a", "img" -> {
                            var href = element.attr("href");
                            var html = element.html();

                            try {
                                var hrefUri = new URI(href);
                                yield Map.entry(hrefUri, html);
                            } catch (URISyntaxException e) {
                                log.error("href is not a valid uri", e);
                                yield null;
                            }
                        }
                        case "video" -> {
                            var src  = element.attr("src");
                            var html = element.html();
                            yield Map.entry(URI.create(src), html);
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + tagName);
                    };
                })
                .filter(entry -> entry != null && entry.getKey().isAbsolute())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
