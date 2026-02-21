package no.clueless.webmention.cli.commands;

import no.clueless.webmention.cli.Command;
import no.clueless.webmention.cli.CommandResult;

import static no.clueless.webmention.cli.ApplicationProperties.ARTIFACT_ID;

/**
 * Prints the help message.
 */
public class Help implements Command {
    /**
     * Constructs a new Help command.
     */
    public Help() {}

    public CommandResult execute(String[] args) {
        System.out.printf("""
                Usage: %s [options]
                
                Options:
                    get-pending-webmentions
                        -u,   --uri <uri>           The URI to check for pending webmentions
                    find-and-send-webmentions
                        -u,   --uri <uri>           The base URI to use
                        -d,   --dir <path>          The root directory to scan
                        -dr,  --dry-run             Show what would happen without sending
                        -r,   --restrictive         Only send webmentions to targets from elements with the attribute "data-webmention"
                    version                         Show the version
                    help                            Show this help message
                
                Environment variables:
                    WEBMENTION_LOG_LEVEL            The desired log level. DEBUG, INFO, WARN, ERROR. Default: INFO
                    WEBMENTION_CLIENT_ID            The OAuth client ID. Optional, but if not present, the user will be prompted for it.
                    WEBMENTION_CLIENT_SECRET        The OAuth client secret. Optional, but if not present, the user will be prompted for it.
                    WEBMENTION_OAUTH_TOKEN_ENDPOINT The OAuth token endpoint. Optional, but if not present, the user will be prompted for it.
                    WEBMENTION_API_ENDPOINT         The webmention API endpoint. Optional, but if not present, the user will be prompted for it.
                    %n""", ARTIFACT_ID);
        return null;
    }
}
