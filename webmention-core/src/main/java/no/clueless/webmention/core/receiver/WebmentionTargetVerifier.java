package no.clueless.webmention.core.receiver;

import no.clueless.webmention.core.WebmentionException;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

@FunctionalInterface
public interface WebmentionTargetVerifier {
    boolean verify(@NotNull URI targetUri) throws WebmentionException;
}
