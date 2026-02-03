package no.clueless.webmention_cli;

import no.clueless.webmention.event.WebmentionEvent;
import no.clueless.webmention.receiver.WebmentionHtmlSourceScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

class WebmentionDirectoryWalkerTest {

    @Test
    void walk(@TempDir Path tempDir) throws IOException {
        // arrange
        var sut = spy(new WebmentionDirectoryWalker(new WebmentionHtmlSourceScanner(), Set.of("htm", "html")));
        var indexHtml = Files.createFile(tempDir.resolve("index.html"));
        Files.createFile(tempDir.resolve("foo.json"));

        Files.writeString(indexHtml, """
                <html>
                    <body>
                        <a href="https://example.com/foo.html">Foo!</a>
                        <a href="https://example.com/bar.html">Bar!</a>
                        <a href="https://example.com/baz.html">Baz!</a>
                    </body>
                </html>
                """);

        // act
        var result = sut.walk(URI.create("https://example.com"), tempDir, new HashSet<>());

        // assert
        assertEquals(Set.of(
                new WebmentionEvent("https://example.com/index.html", "https://example.com/foo.html", "Foo!"),
                new WebmentionEvent("https://example.com/index.html", "https://example.com/bar.html", "Bar!"),
                new WebmentionEvent("https://example.com/index.html", "https://example.com/baz.html", "Baz!")
        ), result);
    }
}