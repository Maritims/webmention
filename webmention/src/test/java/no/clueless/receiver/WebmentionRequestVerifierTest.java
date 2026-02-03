package no.clueless.receiver;

import no.clueless.webmention.WebmentionException;
import no.clueless.webmention.receiver.WebmentionRequestVerifier;
import no.clueless.webmention.receiver.WebmentionTargetVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebmentionRequestVerifierTest {
    WebmentionTargetVerifier  targetVerifier;
    WebmentionRequestVerifier sut;

    @BeforeEach
    void setUp() {
        targetVerifier = mock(WebmentionTargetVerifier.class);
        sut            = new WebmentionRequestVerifier(targetVerifier);
    }

    @Test
    void verify_shouldThrow_whenSourceUriSchemeIsInvalid() {
        var result = assertThrows(WebmentionException.class, () -> sut.verify("foo", "https://example.com/webmention-endpoint"));
        assertEquals("The scheme of URI foo is not supported", result.getMessage());
    }

    @Test
    void verify_shouldThrow_whenTargetUriSchemeIsInvalid() {
        var result = assertThrows(WebmentionException.class, () -> sut.verify("https://example.com/page/1", "bar"));
        assertEquals("The scheme of URI bar is not supported", result.getMessage());
    }

    @Test
    void verify_shouldThrow_whenUrisAreValidButEqualToEachOther() {
        var result = assertThrows(WebmentionException.class, () -> sut.verify("https://example.com", "https://example.com"));
        assertEquals("Source and targetUrl URLs cannot be equal", result.getMessage());
    }

    @Test
    void verify_shouldThrow_whenTargetVerificationDoesNotPass() throws WebmentionException {
        when(targetVerifier.verify(any(URI.class))).thenReturn(false);
        var result = assertThrows(WebmentionException.class, () -> sut.verify("https://example.com/page/1", "https://example.com/webmention-endpoint/2"));
        assertEquals("Target URL https://example.com/webmention-endpoint/2 is not a valid resource for this receiver", result.getMessage());
    }

    @Test
    void verify_shouldReturnTrue_whenVerificationPasses() throws WebmentionException {
        when(targetVerifier.verify(any(URI.class))).thenReturn(true);
        assertTrue(sut.verify("https://example.com/page/1", "https://example.com/webmention-endpoint/2"));
    }
}