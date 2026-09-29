package io.github.docgen.cli;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.github.docgen.core.HwpxDocument;
import io.github.docgen.core.HwpxFormatException;
import io.github.docgen.core.ReplacementResult;
import io.github.docgen.core.TokenValidationException;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    public static int run(String[] args, PrintStream stdout, PrintStream stderr) {
        CommandLineOptions options;
        try {
            options = CommandLineOptions.parse(args);
        } catch (IllegalArgumentException exception) {
            printError(stderr, exception.getMessage());
            return 2;
        }

        if (options.help()) {
            stdout.println(usage());
            return 0;
        }
        if (!Files.isRegularFile(options.templatePath())) {
            printError(stderr, "Template file does not exist: " + options.templatePath());
            return 2;
        }
        if (!Files.isRegularFile(options.dataPath())) {
            printError(stderr, "Token JSON does not exist: " + options.dataPath());
            return 2;
        }

        Map<String, String> tokens;
        try {
            tokens = JsonTokenReader.read(options.dataPath());
        } catch (TokenValidationException exception) {
            printError(stderr, exception.getMessage());
            return 4;
        } catch (java.io.IOException exception) {
            printError(stderr, exception.getMessage());
            return 2;
        }

        HwpxDocument document;
        try {
            document = HwpxDocument.open(options.templatePath());
        } catch (HwpxFormatException exception) {
            printError(stderr, exception.getMessage());
            return 3;
        } catch (java.io.IOException exception) {
            printError(stderr, exception.getMessage());
            return 3;
        }

        try {
            ReplacementResult result;
            try {
                result = document.replaceTokens(tokens, options.allowMissing());
            } catch (TokenValidationException exception) {
                printError(stderr, exception.getMessage());
                return 4;
            } catch (java.io.IOException exception) {
                printError(stderr, exception.getMessage());
                return 3;
            }

            try {
                document.write(options.outputPath(), options.force());
            } catch (java.io.IOException exception) {
                printError(stderr, exception.getMessage());
                return 5;
            }
            stdout.println("Generated " + options.outputPath().toAbsolutePath()
                    + " (replacements=" + result.replacementCount() + ")");
            return 0;
        } finally {
            try {
                document.close();
            } catch (java.io.IOException exception) {
                printError(stderr, exception.getMessage());
            }
        }
    }

    private static void printError(PrintStream stderr, String message) {
        stderr.println("error: " + message);
    }

    private static String usage() {
        return "Usage: java -jar docgen.jar --template <template.hwpx>"
                + " --data <tokens.json> --output <result.hwpx> [--allow-missing] [--force]";
    }
}
