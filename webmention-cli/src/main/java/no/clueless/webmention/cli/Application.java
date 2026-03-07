package no.clueless.webmention.cli;

import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.util.Arrays;

public class Application {
    @NotNull
    private final PrintWriter      writer;
    @NotNull
    private final CommandProcessor processor;

    public Application(@NotNull PrintWriter writer, @NotNull CommandProcessor processor) {
        this.writer    = writer;
        this.processor = processor;
    }

    public Application(@NotNull PrintWriter writer) {
        this.writer = writer;

        var registry = new CommandRegistry()
                .register("get-webmentions", GetWebmentionsCommand::new)
                .register("send-webmentions", SendWebmentionsCommand::new)
                .register("publish-webmention", PublishWebmentionCommand::new)
                .register("unpublish-webmention", UnpublishWebmentionCommand::new)
                .register("delete-webmention", DeleteWebmentionCommand::new);

        registry.register("help", ignored -> new CommandBase() {
            @Override
            public void run() {
                //registry.printHelp();
            }
        });

        this.processor = new CommandProcessor(registry);
    }

    protected void handleEmptyArgs() {
        System.exit(2);
    }

    protected void printHelp() {
        writer.println("webmention-cli: try 'webmention-cli help' for more information");
    }

    protected void printCommandNotFound(String commandName) {
        writer.println("webmention-cli: command not found: " + commandName);
    }

    protected void printCommandNotSpecified() {
        writer.println("webmention-cli: command not specified");
    }

    public void run(String[] args) {
        if (args.length <= 1) {
            writer.println("webmention-cli: try 'webmention-cli help' for more information");
            handleEmptyArgs();
            return;
        }

        try {
            var commandProcessorArgs = Arrays.copyOfRange(args, 1, args.length);
            processor.run(commandProcessorArgs);
        } catch (CommandNotFoundException e) {
            printCommandNotFound(e.getCommandName());
            printHelp();
        } catch (CommandNotSpecifiedException e) {
            printCommandNotSpecified();
            printHelp();
        }
    }

    public static void main(String[] args) {
        new Application(new PrintWriter(System.out)).run(args);
    }
}
