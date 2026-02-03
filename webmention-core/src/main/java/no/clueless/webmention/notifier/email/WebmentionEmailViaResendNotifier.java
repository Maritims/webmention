package no.clueless.webmention.notifier.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.clueless.webmention.notifier.WebmentionNotifier;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

public class WebmentionEmailViaResendNotifier implements WebmentionNotifier<WebmentionEmailNotification> {
    private static final Logger       log                     = LoggerFactory.getLogger(WebmentionEmailViaResendNotifier.class);
    private static final ObjectMapper jsonMapper              = new ObjectMapper();
    private static final String       SENDER_EMAIL_ADDRESS    = Optional.ofNullable(System.getenv("WEBMENTION_SENDER_EMAIL_ADDRESS")).filter(property -> !property.isBlank()).orElseThrow(() -> new IllegalStateException("WEBMENTION_SENDER_EMAIL_ADDRESS must be set"));
    private static final String       RECIPIENT_EMAIL_ADDRESS = Optional.ofNullable(System.getenv("WEBMENTION_RECIPIENT_EMAIL_ADDRESS")).filter(property -> !property.isBlank()).orElseThrow(() -> new IllegalStateException("WEBMENTION_RECIPIENT_EMAIL_ADDRESS must be set"));
    private static final String       EMAIL_SUBJECT           = Optional.ofNullable(System.getenv("WEBMENTION_EMAIL_SUBJECT")).filter(property -> !property.isBlank()).orElse("New webmention pending approval");

    public WebmentionEmailViaResendNotifier() {
        Optional.ofNullable(System.getenv("WEBMENTION_RESEND_API_KEY")).filter(value -> !value.isBlank()).orElseThrow(() -> new IllegalStateException("WEBMENTION_RESEND_API_KEY must be set"));
    }

    @Override
    public WebmentionEmailNotification newNotification(String sourceUrl, String targetUrl, String mentionText) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrl cannot be null or blank");
        }
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("targetUrl cannot be null or blank");
        }

        var body = String.format("""
                        <h1>New webmention</h1>
                        <p><b>Source URL:</b> <a href="%s">%s</a></p>
                        <p><b>Target URL:</b> <a href="%s">%s</a>/p>
                        """, sourceUrl, sourceUrl, targetUrl, targetUrl);
        if (mentionText != null && !mentionText.isBlank()) {
            body += String.format("""
                    <p><b>Mention text:</b></p>
                    <pre><code>%s</code></pre>""", StringEscapeUtils.escapeHtml4(mentionText));
        }

        return new WebmentionEmailNotification(SENDER_EMAIL_ADDRESS, RECIPIENT_EMAIL_ADDRESS, EMAIL_SUBJECT, body, sourceUrl, targetUrl, mentionText);
    }

    @Override
    public void notify(WebmentionEmailNotification notification) {
        try (var httpClient = HttpClient.newHttpClient()) {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + System.getenv("WEBMENTION_RESEND_API_KEY"))
                    .header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(Map.of(
                            "from", notification.senderEmailAddress(),
                            "to", notification.recipientEmailAddress(),
                            "subject", notification.subject(),
                            "html", notification.body()
                    ))))
                    .build();
            var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                log.error("Failed to send email notification: {}", httpResponse.body());
            } else {
                log.info("Sent email notification");
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to send email notification", e);
        }
    }
}
