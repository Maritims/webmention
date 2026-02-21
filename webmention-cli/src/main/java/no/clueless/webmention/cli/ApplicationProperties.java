package no.clueless.webmention.cli;

import java.io.IOException;
import java.time.Instant;
import java.util.Properties;

/**
 * Application properties.
 */
public class ApplicationProperties {
    /**
     * The application artifact ID.
     */
    public static final String  ARTIFACT_ID;
    /**
     * The application version.
     */
    public static final String  VERSION;
    /**
     * The application build time.
     */
    public static       Instant BUILD_TIME;

    static {
        var props = new Properties();
        try (var inputStream = Application.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                throw new RuntimeException("Failed to load application.properties. The file was not found");
            }
            props.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }

        ARTIFACT_ID = props.getProperty("artifact.id");
        VERSION     = props.getProperty("version");
        try {
            BUILD_TIME = Instant.parse(props.getProperty("build.time"));
        } catch (Exception e) {
            BUILD_TIME = null;
        }
    }

    private ApplicationProperties() {
    }
}
