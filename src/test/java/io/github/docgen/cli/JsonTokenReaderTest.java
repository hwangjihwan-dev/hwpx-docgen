package io.github.docgen.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.docgen.core.TokenValidationException;

class JsonTokenReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsFlatStringObject() throws Exception {
        Path json = tempDir.resolve("tokens.json");
        Files.write(json, "{\"title\":\"문서\",\"author\":\"작성자\"}".getBytes(StandardCharsets.UTF_8));

        Map<String, String> tokens = JsonTokenReader.read(json);

        assertEquals("문서", tokens.get("title"));
        assertEquals("작성자", tokens.get("author"));
    }

    @Test
    void rejectsNestedOrNonStringValues() throws Exception {
        Path json = tempDir.resolve("invalid.json");
        Files.write(json, "{\"nested\":{\"value\":\"x\"}}".getBytes(StandardCharsets.UTF_8));

        assertThrows(TokenValidationException.class, () -> JsonTokenReader.read(json));
    }
}
