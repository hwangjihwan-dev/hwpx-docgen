package io.github.docgen.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    @Test
    void templateDoesNotKeepStaleLayoutCachesForPlaceholders() throws Exception {
        Path template = Paths.get("examples", "minimal", "template.hwpx");

        try (ZipFile zip = new ZipFile(template.toFile())) {
            ZipEntry sectionEntry = zip.getEntry("Contents/section0.xml");
            assertNotNull(sectionEntry, "Missing HWPX section: Contents/section0.xml");
            try (InputStream input = zip.getInputStream(sectionEntry)) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                String section = new String(output.toByteArray(), StandardCharsets.UTF_8);
                assertEquals(-1, section.indexOf("<hp:linesegarray"));
            }
        }
    }
}
