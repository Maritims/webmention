package no.clueless.webmention.security;

import no.clueless.webmention.persistence.Client;
import no.clueless.webmention.persistence.ClientRepository;
import org.mindrot.jbcrypt.BCrypt;

public class ClientService {
    private final ClientRepository clientRepository;

    /**
     * Constructor.
     *
     * @param clientRepository the client repository
     * @throws IllegalArgumentException if clientRepository is null
     */
    public ClientService(ClientRepository clientRepository) {
        if (clientRepository == null) {
            throw new IllegalArgumentException("clientRepository cannot be null");
        }
        this.clientRepository = clientRepository;
    }

    /**
     * Validates client credentials.
     *
     * @param clientId     the client id
     * @param clientSecret the client secret
     * @return True, if a client with the given id exists, is enabled, and the secret matches. Otherwise false.
     */
    public boolean isValidClient(String clientId, String clientSecret) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be null or blank");
        }

        var client = clientRepository.findByClientId(clientId).orElseThrow(() -> new IllegalArgumentException("No client with clientId " + clientId + " was found"));
        return client.isEnabled() && BCrypt.checkpw(clientSecret, client.clientSecret());
    }

    /**
     * Creates a new client.
     * @param clientId the client id
     * @param clientSecret the client secret
     * @return the created client
     * @throws IllegalArgumentException if clientId or clientSecret is null or blank, or if a client with the given id already exists
     */
    public Client createClient(String clientId, String clientSecret) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId cannot be null or blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientSecret cannot be null or blank");
        }

        var client = clientRepository.findByClientId(clientId).orElse(null);
        if (client != null) {
            throw new IllegalArgumentException("Client with clientId " + clientId + " already exists");
        }

        var hashedClientSecret = BCrypt.hashpw(clientSecret, BCrypt.gensalt());
        client = Client.newClient(clientId, hashedClientSecret);
        return clientRepository.create(client);
    }
}
