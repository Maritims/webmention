package no.clueless.webmention_javalin;

import no.clueless.webmention.persistence.WebmentionRepository;
import no.clueless.webmention.receiver.WebmentionProcessor;
import no.clueless.webmention.sender.WebmentionSender;

public class WebmentionConfig {
    private String                  endpoint;
    private WebmentionProcessor     webmentionProcessor;
    private WebmentionSender        sender;
    private WebmentionRepository<?> webmentionRepository;
    private boolean                 testMode;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public WebmentionProcessor getProcessor() {
        return webmentionProcessor;
    }

    public void setProcessor(WebmentionProcessor webmentionProcessor) {
        this.webmentionProcessor = webmentionProcessor;
    }

    public WebmentionSender getSender() {
        return sender;
    }

    public void setSender(WebmentionSender sender) {
        this.sender = sender;
    }

    public WebmentionRepository<?> getWebmentionRepository() {
        return webmentionRepository;
    }

    public void setWebmentionRepository(WebmentionRepository<?> webmentionRepository) {
        this.webmentionRepository = webmentionRepository;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }
}
