package no.clueless.webmention;

import no.clueless.webmention.http.SecureHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@SuppressWarnings("unchecked")
class WebmentionEndpointDiscovererTest {
    SecureHttpClient             httpClient;
    WebmentionEndpointDiscoverer sut;

    public static Stream<Arguments> discover_shouldPrefer_findInHeaders() {
        return Stream.of(
                Arguments.of("Link", "<https://example.com/webmention-endpoint>; rel=webmention", "https://example.com/webmention-endpoint"),
                Arguments.of("Link", "<https://example.com/webmention-endpoint>; rel=\"webmention\"", "https://example.com/webmention-endpoint")
        );
    }

    public static Stream<Arguments> findInHtml() {
        return Stream.of(
                Arguments.of("""
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="webmention" href="https://example.com/webmention-endpoint">
                            </head>
                            <body></body>
                        </html>
                        """, "https://example.com/webmention-endpoint"),
                Arguments.of("""
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="foo webmention bar" href="https://example.com/webmention-endpoint">
                            </head>
                            <body></body>
                        </html>
                        """, "https://example.com/webmention-endpoint"),
                Arguments.of("""
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="foo not-webmention bar" href="https://example.com/nonsense">
                            </head>
                            <body></body>
                        </html>
                        """, null)
        );
    }

    static HttpResponse<String> mockHttpResponse(Map<String, List<String>> headers, String body) {
        var httpHeaders  = mock(HttpHeaders.class);
        var httpResponse = mock(HttpResponse.class);
        headers = new HashMap<>(headers);
        headers.put("Content-Type", List.of("text/html"));
        when(httpHeaders.firstValue("Content-Type")).thenReturn(Optional.of(headers.get("Content-Type").getFirst()));
        when(httpHeaders.map()).thenReturn(headers);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpResponse.body()).thenReturn(body);
        return httpResponse;
    }

    public static Stream<Arguments> discover() {
        return Stream.of(
                Arguments.of("Discovery Test #1 - HTTP Link header, unquoted rel, relative URL", mockHttpResponse(Map.of("link", List.of("</webmention-endpoint/1>; rel=webmention")), ""), "https://example.com/webmention-endpoint/1"),
                Arguments.of("Discovery Test #2 - HTTP Link header, unquoted rel, absolute URL", mockHttpResponse(Map.of("link", List.of("<https://example.com/webmention-endpoint/1>; rel=webmention")), ""), "https://example.com/webmention-endpoint/1"),
                Arguments.of("Discovery Test #3 - HTML <link> tag, relative URL", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="webmention" href="/webmention-endpoint">
                            </head>
                            <body></body>
                        </htmL>
                        """), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #4 - HTML <link> tag, absolute URL", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="webmention" href="https://example.com/webmention-endpoint">
                            </head>
                            <body></body>
                        </htmL>
                        """), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #5 - HTML <a> tag, relative URL", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head></head>
                            <body>
                                <a rel="webmention" href="/webmention-endpoint">
                            </body>
                            </html>
                        """), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #6 - HTML <a> tag, absolute URL", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head></head>
                            <body>
                                <a rel="webmention" href="https://example.com/webmention-endpoint">
                            </body>
                            </html>
                        """), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #7 - HTTP Link header with strange casing", mockHttpResponse(Map.of("LiNk", List.of("<https://example.com/webmention-endpoint>; rel=webmention")), ""), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #8 - HTTP Link header, quoted rel", mockHttpResponse(Map.of("link", List.of("<https://example.com/webmention-endpoint>; rel=\"webmention\"")), ""), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #9 - Multiple rel values on a <link> tag", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="foo webmention bar" href="https://example.com/webmention-endpoint">
                            </head>
                            <body></body>
                        </html>
                        """), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #10 - Multiple rel values on a Link header", mockHttpResponse(Map.of("link", List.of("<https://example.com/webmention-endpoint>; rel=foo webmention bar")), ""), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #11 - Multiple no.clueless.webmention.receiver.Webmention endpoints advertised: Link, <link>, <a>", mockHttpResponse(Map.of("link", List.of("</webmention-endpoint>; rel=webmention")), """
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="webmention" href="/nonsense">
                            </head>
                            <body>
                                <a rel="webmention" href="/more-nonsense">foo bar</a>
                            </body>
                        </html>
                        """), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #12 - Checking for exact match of rel=webmention", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="not-webmention" href="/nonsense">
                            </head>
                            <body>
                                <a rel="webmention" href="/webmention-endpoint">foo bar</a>
                            </body>
                        </html>
                        """), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #13 - False endpoint inside an HTML comment", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head></head>
                            <body>
                                <!-- <a rel="webmention" href="/nonsense">foo bar</a> -->
                                <a rel="webmentioN" href="/webmention-endpoint">bar baz</a>
                            </body>
                        </html>
                        """), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #14 - False endpoint in escaped HTML", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head></head>
                            <body>
                                <code>&lt;a rel="webmention" href="/nonsense"&gt;&lt;/a&gt;</code>
                                <a rel="webmention" href="/webmention-endpoint">foo bar</a>
                            </body>
                        </html>
                        """), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #15 - no.clueless.webmention.receiver.Webmention href is an empty string", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="webmention" href="">
                            </head>
                            <body></body>
                        </htmL>
                        """), "https://example.com"),
                Arguments.of("Discovery Test #16 - Multiple no.clueless.webmention.receiver.Webmention endpoints advertised: <a>, <link>", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head>
                            </head>
                            <body>
                                <a rel="webmention" href="/webmention-endpoint">foo bar</a>
                                <link rel="webmention" href="/nonsense">
                            </body>
                        </html>
                        """), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #17 - Multiple no.clueless.webmention.receiver.Webmention endpoints advertised: <link>, <a>", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head>
                            </head>
                            <body>
                                <link rel="webmention" href="/webmention-endpoint">
                                <a rel="webmention" href="/nonsense">foo bar</a>
                            </body>
                        </html>
                        """), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #18 - Multiple HTTP Link headers", mockHttpResponse(Map.of("link", List.of("<https://example.com/nonsense>; rel=other", "<https://example.com/webmention-endpoint>; rel=webmention")), ""), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #19 - Single HTTP Link header with multiple values", mockHttpResponse(Map.of("link", List.of("<https://example.com/nonsense>; rel=\"other\", <https://example.com/webmention-endpoint>; rel=\"webmention\"")), ""), "https://example.com/webmention-endpoint"),
                Arguments.of("Discovery Test #20 - Link tag with no href attribute", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="webmention" href="">
                            </head>
                            <body></body>
                        </html>
                        """), "https://example.com"),
                Arguments.of("Discovery Test #21 - no.clueless.webmention.receiver.Webmention endpoint has query string parameters", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="webmention" href="/webmention-endpoint?foo=bar">
                            </head>
                            <body></body>
                        </html>
                        """), "https://example.com/webmention-endpoint?foo=bar"),
                Arguments.of("Discovery Test #22 - no.clueless.webmention.receiver.Webmention endpoint is relative to the path", mockHttpResponse(Map.of(), """
                        <!DOCTYPE html>
                        <html>
                            <head>
                                <link rel="webmention" href="foo/webmention-endpoint">
                            </head>
                            <body></body>
                        </html>
                        """), "https://example.com/foo/webmention-endpoint")
        );
    }

    @BeforeEach
    void setUp() {
        httpClient = mock(SecureHttpClient.class);
        sut        = spy(new WebmentionEndpointDiscoverer(httpClient));
    }

    @ParameterizedTest
    @MethodSource
    void discover(@SuppressWarnings("unused") String name, HttpResponse<String> httpResponse, String expected) throws IOException, InterruptedException, UnexpectedContentTypeException {
        // arrange
        when(httpClient.send(any(HttpRequest.class))).thenReturn(httpResponse);

        // act
        var result = sut.discover(URI.create("https://example.com")).orElse(null);

        // assert
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"link", "Link"})
    void findInHeaders(String linkHeaderName) {
        // arrange
        var webmentionEndpoint = "https://example.com/webmention-endpoint";
        var httpHeaders        = mock(HttpHeaders.class);
        when(httpHeaders.map()).thenReturn(Map.of(linkHeaderName, List.of("<" + webmentionEndpoint + ">; rel=\"webmention\"")));

        // act
        var result = sut.findInHeaders(httpHeaders).orElse(null);

        // assert
        assertNotNull(result);
        assertEquals(webmentionEndpoint, result);
    }

    @ParameterizedTest
    @MethodSource
    void findInHtml(String html, String expected) {
        // arrange
        var document = Jsoup.parse(html);

        // act
        var result = sut.findInHtml(document).orElse(null);

        // assert
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @MethodSource
    void discover_shouldPrefer_findInHeaders(String linkHeaderName, String linkHeaderValue, String expected) throws IOException, InterruptedException, UnexpectedContentTypeException {
        // arrange
        var httpResponse = mock(HttpResponse.class);
        var httpHeaders  = mock(HttpHeaders.class);
        when(httpHeaders.firstValue("Content-Type")).thenReturn(Optional.of("text/html"));
        when(httpHeaders.map()).thenReturn(Map.of(linkHeaderName, List.of(linkHeaderValue)));
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpClient.send(any(HttpRequest.class))).thenReturn(httpResponse);

        // act
        var result = sut.discover(URI.create("https://example.com")).orElse(null);

        // assert
        verify(sut, never()).findInHtml(nullable(Document.class));
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void discover_shouldPrefer_findInHtml_when_findInHeaders_is_empty() throws IOException, InterruptedException, UnexpectedContentTypeException {
        // arrange
        var httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.firstValue("Content-Length")).thenReturn(Optional.of("10"));
        when(httpHeaders.firstValue("Content-Type")).thenReturn(Optional.of("text/html"));

        var httpResponse = mock(HttpResponse.class);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpResponse.body()).thenReturn("foobar");

        var document = mock(Document.class);

        doReturn(document).when(sut).parseHtml(nullable(String.class));
        doReturn(Optional.empty()).when(sut).findInHeaders(eq(httpHeaders));
        doReturn(Optional.of("https://example.com/webmention-endpoint")).when(sut).findInHtml(any(Document.class));
        when(httpClient.send(any(HttpRequest.class))).thenReturn(httpResponse);

        // act
        var result = sut.discover(URI.create("https://example.com")).orElse(null);

        // assert
        verify(sut, times(1)).findInHeaders(nullable(HttpHeaders.class));
        verify(sut, times(1)).findInHtml(any(Document.class));
        assertNotNull(result);
        assertEquals("https://example.com/webmention-endpoint", result);
    }

    @Test
    void discover_shouldResolveRelativeUrl_relativeToTargetUrl() throws IOException, InterruptedException, UnexpectedContentTypeException {
        // arrange
        var httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.firstValue("Content-Type")).thenReturn(Optional.of("text/html"));
        var httpResponse = mock(HttpResponse.class);
        when(httpResponse.headers()).thenReturn(httpHeaders);

        when(httpClient.send(any(HttpRequest.class))).thenReturn(httpResponse);
        doReturn(Optional.of("/webmention-endpoint")).when(sut).findInHeaders(eq(httpHeaders));

        // act
        var result = sut.discover(URI.create("https://example.com/hello-world?foo=bar")).orElse(null);

        // assert
        assertNotNull(result);
        assertEquals("https://example.com/webmention-endpoint", result);
    }

    @Test
    void discover_shouldResolveEmptyUrl_toTargetUrl() throws IOException, InterruptedException, UnexpectedContentTypeException {
        // arrange
        var httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.firstValue("Content-Type")).thenReturn(Optional.of("text/html"));
        var httpResponse = mock(HttpResponse.class);
        when(httpResponse.headers()).thenReturn(httpHeaders);

        when(httpClient.send(any(HttpRequest.class))).thenReturn(httpResponse);
        doReturn(Optional.of("")).when(sut).findInHeaders(nullable(HttpHeaders.class));

        // act
        var result = sut.discover(URI.create("https://example.com/hello-world")).orElse(null);

        // assert
        assertNotNull(result);
        assertEquals("https://example.com/hello-world", result);
    }

    @Test
    void discover_shouldThrow_whenContentTypeIsMissing() throws IOException, InterruptedException {
        var httpResponse = mock(HttpResponse.class);
        when(httpClient.send(any(HttpRequest.class))).thenReturn(httpResponse);

        assertThrows(RuntimeException.class, () -> sut.discover(URI.create("https://example.com/webmention-endpoint")).orElse(null));
    }

    @Test
    void discover_shouldThrow_whenContentTypeIsNotTextHtml() throws IOException, InterruptedException {
        var httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.firstValue("Content-Type")).thenReturn(Optional.of("text/plain"));
        var httpResponse = mock(HttpResponse.class);
        when(httpResponse.headers()).thenReturn(httpHeaders);
        when(httpClient.send(any(HttpRequest.class))).thenReturn(httpResponse);

        assertThrows(UnexpectedContentTypeException.class, () -> sut.discover(URI.create("https://example.com/webmention-endpoint")).orElse(null));
    }
}