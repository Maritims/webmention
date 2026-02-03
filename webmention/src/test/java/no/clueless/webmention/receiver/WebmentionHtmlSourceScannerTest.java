package no.clueless.webmention.receiver;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebmentionHtmlSourceScannerTest {

    @Test
    void findAllMentions_shouldOnlyIncludeAbsoluteURLs() {
        // arrange
        var body = """
                <!DOCTYPE html>
                <html>
                    <head>
                        <title>index.html</title>
                    </head>
                    <body>
                        <a href="/index.html">index.html</a>
                        <a href="https://example.com">https://example.com</a>
                        <a href="https://example.com/hello-world.html">https://example.com/hello-world.html</a>
                    </body>
                </html>
                """;
        var sut = new WebmentionHtmlSourceScanner();

        // act
        var result = sut.findAllMentions(body);

        // assert
        assertEquals(Map.of(URI.create("https://example.com"), "https://example.com", URI.create("https://example.com/hello-world.html"), "https://example.com/hello-world.html"), result);
    }
}