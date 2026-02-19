package no.clueless.oauth;

@FunctionalInterface
public interface TokenGenerator {
    String generate(OAuthClient client);
}
