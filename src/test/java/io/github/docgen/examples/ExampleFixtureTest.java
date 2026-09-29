package io.github.docgen.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;

class ExampleFixtureTest {

    @Test
    void containsTheMinimumHancomPackageParts() throws Exception {
        Path template = Paths.get("examples", "minimal", "template.hwpx");
        Set<String> required = new HashSet<>(Arrays.asList(
                "mimetype",
                "version.xml",
                "settings.xml",
                "Contents/content.hpf",
                "Contents/header.xml",
                "Contents/section0.xml",
                "META-INF/container.xml",
                "META-INF/container.rdf",
                "META-INF/manifest.xml",
                "Preview/PrvText.txt"));

        try (ZipFile zip = new ZipFile(template.toFile())) {
            ZipEntry first = zip.entries().nextElement();
            assertEquals("mimetype", first.getName());
            assertEquals(ZipEntry.STORED, first.getMethod());
            for (String entry : required) {
                assertNotNull(zip.getEntry(entry), "Missing HWPX package entry: " + entry);
            }
        }
    }
}
