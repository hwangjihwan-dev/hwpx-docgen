package io.github.docgen.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HwpxDocumentTest {

    @TempDir
    Path tempDir;

    @Test
    void opensMinimalHwpxAndFindsSections() throws Exception {
        Path template = HwpxTestFixtures.createMinimalHwpx(tempDir, HwpxTestFixtures.section("{{title}}"));

        HwpxDocument document = HwpxDocument.open(template);
        try {
            assertEquals(1, document.sectionFiles().size());
            assertEquals("section0.xml", document.sectionFiles().get(0).getFileName().toString());
        } finally {
            document.close();
        }
    }

    @Test
    void rejectsHwpxWithoutContents() throws Exception {
        Path invalid = HwpxTestFixtures.createHwpxWithoutContents(tempDir);

        assertThrows(HwpxFormatException.class, () -> HwpxDocument.open(invalid));
    }

    @Test
    void closeRemovesTemporaryWorkspace() throws Exception {
        Path template = HwpxTestFixtures.createMinimalHwpx(tempDir, HwpxTestFixtures.section("text"));

        HwpxDocument document = HwpxDocument.open(template);
        Path workspace = document.sectionFiles().get(0).getParent().getParent();
        assertTrue(Files.exists(workspace));

        document.close();

        assertFalse(Files.exists(workspace));
    }

    @Test
    void refusesExistingOutputUnlessOverwriteIsEnabled() throws Exception {
        Path template = HwpxTestFixtures.createMinimalHwpx(tempDir, HwpxTestFixtures.section("text"));
        Path output = tempDir.resolve("existing.hwpx");
        Files.write(output, "keep".getBytes(StandardCharsets.UTF_8));

        HwpxDocument document = HwpxDocument.open(template);
        try {
            assertThrows(IOException.class, () -> document.write(output, false));
            assertEquals("keep", new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
        } finally {
            document.close();
        }
    }
}
