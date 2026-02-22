package no.clueless.webmention.cli;

public class Application {
    private Application() {
    }

    public static void main(String[] args) {
        var commandProcessor = new CommandProcessor();
        commandProcessor.process(args);
    }
}
