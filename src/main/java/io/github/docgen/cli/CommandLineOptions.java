package io.github.docgen.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public final class CommandLineOptions {
    private final Path templatePath;
    private final Path dataPath;
    private final Path outputPath;
    private final boolean allowMissing;
    private final boolean force;
    private final boolean help;

    private CommandLineOptions(Path templatePath, Path dataPath, Path outputPath,
            boolean allowMissing, boolean force, boolean help) {
        this.templatePath = templatePath;
        this.dataPath = dataPath;
        this.outputPath = outputPath;
        this.allowMissing = allowMissing;
        this.force = force;
        this.help = help;
    }

    public static CommandLineOptions parse(String[] args) throws IllegalArgumentException {
        Path template = null;
        Path data = null;
        Path output = null;
        boolean allowMissing = false;
        boolean force = false;
        boolean help = false;
        Set<String> seen = new HashSet<>();

        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if ("--help".equals(argument)) {
                if (!seen.add(argument)) {
                    throw new IllegalArgumentException("Duplicate option: " + argument);
                }
                help = true;
            } else if ("--allow-missing".equals(argument)) {
                if (!seen.add(argument)) {
                    throw new IllegalArgumentException("Duplicate option: " + argument);
                }
                allowMissing = true;
            } else if ("--force".equals(argument)) {
                if (!seen.add(argument)) {
                    throw new IllegalArgumentException("Duplicate option: " + argument);
                }
                force = true;
            } else if ("--template".equals(argument)) {
                template = Paths.get(readValue(args, ++i, argument));
                rejectDuplicate(seen, argument);
            } else if ("--data".equals(argument)) {
                data = Paths.get(readValue(args, ++i, argument));
                rejectDuplicate(seen, argument);
            } else if ("--output".equals(argument)) {
                output = Paths.get(readValue(args, ++i, argument));
                rejectDuplicate(seen, argument);
            } else {
                throw new IllegalArgumentException("Unknown option: " + argument);
            }
        }

        if (help) {
            return new CommandLineOptions(template, data, output, allowMissing, force, true);
        }
        if (template == null || data == null || output == null) {
            throw new IllegalArgumentException("--template, --data, and --output are required");
        }
        return new CommandLineOptions(template, data, output, allowMissing, force, false);
    }

    public Path templatePath() {
        return templatePath;
    }

    public Path dataPath() {
        return dataPath;
    }

    public Path outputPath() {
        return outputPath;
    }

    public boolean allowMissing() {
        return allowMissing;
    }

    public boolean force() {
        return force;
    }

    public boolean help() {
        return help;
    }

    private static String readValue(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }

    private static void rejectDuplicate(Set<String> seen, String option) {
        if (!seen.add(option)) {
            throw new IllegalArgumentException("Duplicate option: " + option);
        }
    }
}
