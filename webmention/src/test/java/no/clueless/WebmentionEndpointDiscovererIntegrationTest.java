package no.clueless;

import no.clueless.webmention.UnexpectedContentTypeException;
import no.clueless.webmention.WebmentionEndpointDiscoverer;
import no.clueless.webmention.http.SecureHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class WebmentionEndpointDiscovererIntegrationTest {
    SecureHttpClient             httpClient;
    WebmentionEndpointDiscoverer sut;

    public static Stream<Arguments> discover() {
        return Stream.of(
                Arguments.of("Discovery Test #1 - HTTP Link header, unquoted rel, relative URL", "https://webmention.rocks/test/1", "https://webmention.rocks/test/1/webmention"),
                Arguments.of("Discovery Test #2 - HTTP Link header, unquoted rel, absolute", "https://webmention.rocks/test/2", "https://webmention.rocks/test/2/webmention"),
                Arguments.of("Discovery Test #3 - HTML <link> tag, relative URL", "https://webmention.rocks/test/3", "https://webmention.rocks/test/3/webmention"),
                Arguments.of("Discovery Test #4 - HTML <link> tag, absolute URL", "https://webmention.rocks/test/4", "https://webmention.rocks/test/4/webmention"),
                Arguments.of("Discovery Test #5 - HTML <a> tag, relative URL", "https://webmention.rocks/test/5", "https://webmention.rocks/test/5/webmention"),
                Arguments.of("Discovery Test #6 - HTML <a> tag, absolute URL", "https://webmention.rocks/test/6", "https://webmention.rocks/test/6/webmention"),
                Arguments.of("Discovery Test #7 - HTTP Link header with strange casing", "https://webmention.rocks/test/7", "https://webmention.rocks/test/7/webmention"),
                Arguments.of("Discovery Test #8 - HTTP Link header, quoted rel", "https://webmention.rocks/test/8", "https://webmention.rocks/test/8/webmention"),
                Arguments.of("Discovery Test #9 - Multiple rel values on a <link> tag", "https://webmention.rocks/test/9", "https://webmention.rocks/test/9/webmention"),
                Arguments.of("Discovery Test #10 - Multiple rel values on a Link header", "https://webmention.rocks/test/10", "https://webmention.rocks/test/10/webmention"),
                Arguments.of("Discovery Test #11 - Multiple no.clueless.webmention.receiver.Webmention endpoints advertised: Link, <link>, <a>", "https://webmention.rocks/test/11", "https://webmention.rocks/test/11/webmention"),
                Arguments.of("Discovery Test #12 - Checking for the exact match of rel=webmention", "https://webmention.rocks/test/12", "https://webmention.rocks/test/12/webmention"),
                Arguments.of("Discovery Test #13 - False endpoint inside an HTML comment", "https://webmention.rocks/test/13", "https://webmention.rocks/test/13/webmention"),
                Arguments.of("Discovery Test #14 - False endpoint in escaped HTML", "https://webmention.rocks/test/14", "https://webmention.rocks/test/14/webmention"),
                Arguments.of("Discovery Test #15 - no.clueless.webmention.receiver.Webmention href is an empty string", "https://webmention.rocks/test/15", "https://webmention.rocks/test/15"),
                Arguments.of("Discovery Test #16 - Multiple no.clueless.webmention.receiver.Webmention endpoints advertised: <a>, <link>", "https://webmention.rocks/test/16", "https://webmention.rocks/test/16/webmention"),
                Arguments.of("Discovery Test #17 - Multiple no.clueless.webmention.receiver.Webmention endpoints advertised: <link>, <a>", "https://webmention.rocks/test/17", "https://webmention.rocks/test/17/webmention"),
                Arguments.of("Discovery Test #18 - Multiple HTTP Link headers", "https://webmention.rocks/test/18", "https://webmention.rocks/test/18/webmention"),
                Arguments.of("Discovery Test #19 - Single HTTP Link header with multiple values", "https://webmention.rocks/test/19", "https://webmention.rocks/test/19/webmention"),
                Arguments.of("Discovery Test #20 - Link tag with no href attribute", "https://webmention.rocks/test/20", "https://webmention.rocks/test/20"),
                Arguments.of("Discovery Test #21 - no.clueless.webmention.receiver.Webmention endpoint has query string parameters", "https://webmention.rocks/test/21", "https://webmention.rocks/test/21/webmention?query=yes"),
                Arguments.of("Discovery Test #22 - no.clueless.webmention.receiver.Webmention endpoint is relative to the path", "https://webmention.rocks/test/22", "https://webmention.rocks/test/22/22/webmention")
                // webmention.rocks generates a unique code per request, so performing an assertion is pointless. There's also no point in testing that HttpClient can follow redirects, because we know that it does as long as we configure it to do so.
                //Arguments.of("Discovery Test #23 - no.clueless.webmention.receiver.Webmention targetUrl is a redirect and the endpoint is relative", "https://webmention.rocks/test/23/page", "https://webmention.rocks/test/23/webmention")
        );
    }

    @BeforeEach
    void setUp() {
        httpClient = SecureHttpClient.newClient(Duration.ofSeconds(5), true);
        sut        = new WebmentionEndpointDiscoverer(httpClient);
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("{0}")
    void discover(@SuppressWarnings("unused") String name, String targetUrl, String expected) throws UnexpectedContentTypeException {
        // act
        var result = sut.discover(URI.create(targetUrl)).orElse(null);

        // assert
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void discover_kode24() {
        assertDoesNotThrow(() -> sut.discover(URI.create("https://kode24.no")));
    }
}