package no.clueless.webmention.cli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.util.Properties;

public class ApplicationProperties {
    @NotNull
    public static final String ARTIFACT_ID;
    @NotNull
    public static final String VERSION;
    @Nullable
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
