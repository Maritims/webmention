package no.clueless.webmention.cli.commands;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Command(name = "help", description = "Shows all available commands and their parameters", parameters = {})
public class HelpCommand extends CommandBase {
    @NotNull
    private final List<Command> commands;

    public HelpCommand(@NotNull List<Command> commands) {
        this.commands = commands;
    }

    static class CommandParameterTextCollector implements Collector<CommandParameter, List<List<String>>, String> {
        private final int firstColumnOffset;
        private final int secondColumnOffset;
        private       int maxFirstColumnWidth;

        CommandParameterTextCollector(int firstColumnOffset, int secondColumnOffset) {
            if (firstColumnOffset < 0) {
                throw new IllegalArgumentException("firstColumnOffset must be greater than or equal to 0");
            }
            if (secondColumnOffset < 0) {
                throw new IllegalArgumentException("secondColumnOffset must be greater than or equal to 0");
            }
            this.firstColumnOffset  = firstColumnOffset;
            this.secondColumnOffset = secondColumnOffset;
        }

        @Override
        public Supplier<List<List<String>>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<List<List<String>>, CommandParameter> accumulator() {
            return (list, cp) -> {
                var firstColumn  = String.format(" -%s, --%s", cp.shortName(), cp.longName());
                var secondColumn = String.format("%s%s", cp.description(), cp.required() ? " (required)" : "(optional, defaults to " + cp.defaultValue() + ")");
                maxFirstColumnWidth = Math.max(maxFirstColumnWidth, firstColumn.length());
                list.add(List.of(firstColumn, secondColumn));
            };
        }

        @Override
        public BinaryOperator<List<List<String>>> combiner() {
            return (list1, list2) -> {
                list1.addAll(list2);
                return list1;
            };
        }

        @Override
        public Function<List<List<String>>, String> finisher() {
            return list -> list.stream()
                    .map(line -> {
                        var firstColumnOffsetSpaces = String.format("%" + firstColumnOffset + "s", "");
                        return String.format("%s%-" + (secondColumnOffset + 1) + "s %s", firstColumnOffsetSpaces, line.get(0), line.get(1));
                    })
                    .collect(Collectors.joining("\n"));
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of(Characteristics.UNORDERED);
        }
    }

    static class CommandTextCollector implements Collector<Command, List<Command>, String> {
        private final int offset;
        private       int longestCommandNameLength;

        CommandTextCollector(int offset) {
            if (offset < 0) {
                throw new IllegalArgumentException("offset must be greater than or equal to 0");
            }
            this.offset = offset;
        }

        @Override
        public Supplier<List<Command>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<List<Command>, Command> accumulator() {
            return (commands, command) -> {
                longestCommandNameLength = Math.max(longestCommandNameLength, command.name().length());
                commands.add(command);
            };
        }

        @Override
        public BinaryOperator<List<Command>> combiner() {
            return (list1, list2) -> {
                list1.addAll(list2);
                return list1;
            };
        }

        @Override
        public Function<List<Command>, String> finisher() {
            return commands -> commands.stream()
                    .map(command -> {
                        var spaces         = String.format("%" + offset + "s", "");
                        var firstLine      = String.format("%s%-" + (longestCommandNameLength + 1) + "s %s", spaces, command.name(), command.description());
                        var parameterLines = Arrays.stream(command.parameters()).collect(new CommandParameterTextCollector(offset * 2, longestCommandNameLength - offset));
                        return firstLine + "\n" + parameterLines;
                    })
                    .collect(Collectors.joining("\n"));
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of(Characteristics.UNORDERED);
        }
    }

    @Override
    public void run() {
        var text = commands.stream().collect(new CommandTextCollector(2));
        System.out.println("Usage: webmention-cli <command> [options...]\n" + text);
    }
}
