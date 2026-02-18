package no.clueless.webmention.javalin;

import io.javalin.plugin.Plugin;
import no.clueless.webmention.persistence.ClientRepository;
import no.clueless.webmention.security.ClientService;

public class OAuth2Config extends Plugin<OAuth2Config> {
    private String           issuer;
    private long             accessTokenValiditySeconds;
    private String           jwtSecret;
    private ClientRepository clientRepository;
    private ClientService    clientService;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    public void setAccessTokenValiditySeconds(long accessTokenValiditySeconds) {
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public ClientRepository getClientRepository() {
        return clientRepository;
    }

    public void setClientRepository(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public ClientService getClientValidator() {
        return clientService;
    }

    public void setClientService(ClientService clientService) {
        this.clientService = clientService;
    }
}
