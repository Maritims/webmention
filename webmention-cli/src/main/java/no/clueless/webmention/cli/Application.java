package no.clueless.webmention.cli;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Arrays;

public class Application {
    private static final Logger           log = org.slf4j.LoggerFactory.getLogger(Application.class);
    @NotNull
    private final        CommandProcessor processor;

    public Application(@NotNull CommandProcessor processor) {
        this.processor          = processor;
    }

    public Application() {
        this(new CommandProcessor(new CommandRegistry()
                .register("get", GetWebmentionsCommand::new)
                .register("send", SendWebmentionsCommand::new)
                .register("publish", PublishWebmentionCommand::new)
                .register("unpublish", UnpublishWebmentionCommand::new)
                .register("delete", DeleteWebmentionCommand::new)));
    }

    protected void handleEmptyArgs() {
        System.exit(2);
    }

    protected void printHelp() {
        System.out.println("webmention-cli: try 'webmention-cli help' for more information");
    }

    protected void printCommandNotFound(String commandName) {
        System.err.println("webmention-cli: command not found: " + commandName);
    }

    protected void printCommandNotSpecified() {
        System.err.println("webmention-cli: command not specified");
    }

    protected void printMissingRequiredParameter(String commandName, String parameterName) {
        System.err.println("webmention-cli " + commandName + ": missing required parameter: " + parameterName);
    }

    protected void printInvalidParameterValue(String commandName, String parameterName) {
        System.err.println("webmention-cli " + commandName + ": invalid value for parameter: " + parameterName);
    }

    public void run(String[] args) {
        if (log.isDebugEnabled()) {
            log.debug("Running with args: {}", Arrays.toString(args));
        }

        if (args.length == 0) {
            printCommandNotSpecified();
            printHelp();
            handleEmptyArgs();
            return;
        }

        try {
            processor.run(args);
        } catch (CommandNotFoundException e) {
            printCommandNotFound(e.getCommandName());
            printHelp();
        } catch (MissingRequiredParameter e) {
            printMissingRequiredParameter(e.getCommandName(), e.getParameterName());
            printHelp();
            return;
        } catch (InvalidParameterValueException e) {
            printInvalidParameterValue(e.getCommandName(), e.getParameterName());
            printHelp();
            return;
        }
    }

    public static void main(String[] args) {
        new Application().run(args);
    }
}
