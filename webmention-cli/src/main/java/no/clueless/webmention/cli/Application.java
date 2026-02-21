package no.clueless.webmention.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entry point.
 */
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private Application() {
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        var commandProcessor = new CommandProcessor();
        commandProcessor.process(args);
    }
}
