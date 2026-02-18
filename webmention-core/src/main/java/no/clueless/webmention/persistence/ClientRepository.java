package no.clueless.webmention.persistence;

import java.util.Optional;

public interface ClientRepository extends Repository<Client, Integer> {
    Optional<Client> findByClientId(String clientId);
}
