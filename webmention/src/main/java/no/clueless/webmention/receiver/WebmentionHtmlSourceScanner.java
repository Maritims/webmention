package no.clueless.webmention.receiver;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
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
    public Optional<String> findTargetUrlMention(String body, String targetUrl) {
        var document = Jsoup.parse(body);
        var elements = document.select("a[href=\"" + targetUrl + "\"], img[href=\"" + targetUrl + "\"], video[src=\"" + targetUrl + "\"]");
        return elements.stream()
                .map(this::resolveText)
                .findFirst();
    }

    public Map<URI, String> findAllMentions(String body) {
        var document = Jsoup.parse(body);
        var elements = document.select("a[href], img[href], video[src]");
        return elements.stream()
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
                            var src = element.attr("src");
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
