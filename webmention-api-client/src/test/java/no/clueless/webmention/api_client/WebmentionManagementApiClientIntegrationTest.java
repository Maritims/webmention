package no.clueless.webmention.api_client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.mockito.Mockito.*;

class WebmentionManagementApiClientIntegrationTest {
    WebmentionManagementApiClient sut;

    @BeforeEach
    void setUp() {
        sut        = spy(new WebmentionManagementApiClient(URI.create("http://localhost:7070"), "/oauth/token", "/webmention/manage"));
    }

    @Test
    void getWebmentions() {
        var webmentions = sut.getWebmentions(new Pagination(13, 37), true);
        System.out.println(webmentions);
    }

    @Test
    void publishWebmention() {
    }

    @Test
    void unpublishWebmention() {
    }

    @Test
    void deleteWebmention() {
    }
}