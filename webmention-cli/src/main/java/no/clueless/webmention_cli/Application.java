package no.clueless.webmention_cli;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

public class Application {
    private static final String  ARTIFACT_ID;
    private static final String  VERSION;
    private static final Instant BUILD_TIME;

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
        BUILD_TIME  = Instant.parse(props.getProperty("build.time"));
    }

    private static void printUsage() {
        System.out.printf("""
                Usage: %s [options]
                Options:
                  -u,   --uri <uri>     The base URI to use
                  -d,   --dir <path>    The root directory to scan
                  -dr,  --dry-run       Show what would happen without sending
                  -v,   --version       Show the version
                  -h,   --help          Show this help message
                %n""", ARTIFACT_ID);
    }

    public static void main(String[] args) {
        String uriStr = null;
        String dirStr = null;
        var    dryRun = false;

        for (var i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--uri", "-u" -> {
                    if (++i < args.length) {
                        uriStr = args[i];
                    }
                }
                case "--dir", "-d" -> {
                    if (++i < args.length) {
                        dirStr = args[i];
                    }
                }
                case "--dry-run", "-dr" -> dryRun = true;
                case "--version", "-v" -> {
                    System.out.println(ARTIFACT_ID + " " + VERSION + " (" + BUILD_TIME + ")");
                    return;
                }
                case "--help", "-h" -> {
                    printUsage();
                    return;
                }
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    printUsage();
                    System.exit(1);
                }
            }
        }

        if (uriStr == null || dirStr == null) {
            System.err.println("Both --uri and --dir are required");
            printUsage();
            System.exit(1);
        }

        try {
            final var rootDir = Path.of(dirStr);
            final var baseUri = new URI(uriStr);

            new WebmentionCli().findAndSendWebmentions(baseUri, rootDir, dryRun);
        } catch (URISyntaxException e) {
            System.err.println("Invalid URI: " + uriStr);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Execution failed: " + e);
            System.exit(1);
        }
    }
}
