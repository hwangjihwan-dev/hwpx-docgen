package io.github.docgen.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesRequiredPathsAndFlags() {
        CommandLineOptions options = CommandLineOptions.parse(new String[] {
                "--template", "template.hwpx",
                "--data", "tokens.json",
                "--output", "result.hwpx",
                "--allow-missing",
                "--force"
        });

        assertEquals("template.hwpx", options.templatePath().toString());
        assertEquals("tokens.json", options.dataPath().toString());
        assertEquals("result.hwpx", options.outputPath().toString());
        assertEquals(true, options.allowMissing());
        assertEquals(true, options.force());
    }

    @Test
    void rejectsMissingRequiredOption() {
        assertThrows(IllegalArgumentException.class,
                () -> CommandLineOptions.parse(new String[] {"--template", "template.hwpx"}));
    }

    @Test
    void returnsZeroOnSuccessfulRender() throws Exception {
        Path template = createTemplate("Hello {{title}}");
        Path data = writeJson("{\"title\":\"world\"}");
        Path output = tempDir.resolve("result.hwpx");

        RunResult result = run("--template", template.toString(), "--data", data.toString(), "--output", output.toString());

        assertEquals(0, result.code);
        assertFalse(result.stderr.contains("error"));
        assertTrue(Files.exists(output));
    }

    @Test
    void returnsArgumentErrorCode() throws Exception {
        RunResult result = run("--template", "missing.hwpx");

        assertEquals(2, result.code);
    }

    @Test
    void returnsFormatErrorCode() throws Exception {
        Path template = tempDir.resolve("invalid.hwpx");
        createZip(template, "mimetype", "application/hwp+zip");
        Path data = writeJson("{\"title\":\"world\"}");
        Path output = tempDir.resolve("result.hwpx");

        RunResult result = run("--template", template.toString(), "--data", data.toString(), "--output", output.toString());

        assertEquals(3, result.code);
    }

    @Test
    void returnsTokenErrorCodeForMissingTokens() throws Exception {
        Path template = createTemplate("Hello {{missing}}");
        Path data = writeJson("{}");
        Path output = tempDir.resolve("result.hwpx");

        RunResult result = run("--template", template.toString(), "--data", data.toString(), "--output", output.toString());

        assertEquals(4, result.code);
        assertFalse(Files.exists(output));
    }

    @Test
    void returnsOutputErrorCode() throws Exception {
        Path template = createTemplate("Hello {{title}}");
        Path data = writeJson("{\"title\":\"world\"}");
        Path output = tempDir.resolve("result.hwpx");
        Files.write(output, "existing".getBytes(StandardCharsets.UTF_8));

        RunResult result = run("--template", template.toString(), "--data", data.toString(), "--output", output.toString());

        assertEquals(5, result.code);
        assertEquals("existing", new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
    }

    private RunResult run(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int code = Main.run(args, new PrintStream(stdout), new PrintStream(stderr));
        return new RunResult(code, stdout.toString(), stderr.toString());
    }

    private Path createTemplate(String text) throws Exception {
        Path template = tempDir.resolve("template-" + Files.createTempFile("name-", ".hwpx").getFileName());
        String section = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<hp:section xmlns:hp=\"http://www.hancom.co.kr/hwpml/2011/paragraph\">"
                + "<hp:p><hp:run><hp:t>" + text + "</hp:t></hp:run></hp:p></hp:section>";
        createZip(template, "mimetype", "application/hwp+zip", "Contents/section0.xml", section);
        return template;
    }

    private Path writeJson(String value) throws Exception {
        Path json = Files.createTempFile(tempDir, "tokens-", ".json");
        Files.write(json, value.getBytes(StandardCharsets.UTF_8));
        return json;
    }

    private static void createZip(Path archive, String... pairs) throws Exception {
        try (OutputStream output = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            for (int i = 0; i < pairs.length; i += 2) {
                zip.putNextEntry(new ZipEntry(pairs[i]));
                zip.write(pairs[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    private static final class RunResult {
        private final int code;
        private final String stdout;
        private final String stderr;

        private RunResult(int code, String stdout, String stderr) {
            this.code = code;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
