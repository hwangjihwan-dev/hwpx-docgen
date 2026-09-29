package io.github.docgen.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HwpxArchiveTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsEntriesIntoDestination() throws Exception {
        Path archive = tempDir.resolve("input.hwpx");
        createZip(archive,
                new Entry("mimetype", "application/hwp+zip"),
                new Entry("Contents/section0.xml", "<section/>"));

        Path destination = tempDir.resolve("extracted");
        HwpxArchive.extract(archive, destination);

        assertEquals("application/hwp+zip", readUtf8(destination.resolve("mimetype")));
        assertEquals("<section/>", readUtf8(destination.resolve("Contents/section0.xml")));
    }

    @Test
    void rejectsZipSlipEntry() throws Exception {
        Path archive = tempDir.resolve("unsafe.hwpx");
        createZip(archive, new Entry("../outside.txt", "should not be written"));

        Path destination = tempDir.resolve("extracted");
        assertThrows(IOException.class, () -> HwpxArchive.extract(archive, destination));
        assertFalse(Files.exists(tempDir.resolve("outside.txt")));
    }

    @Test
    void createsRoundTrippableArchive() throws Exception {
        Path source = tempDir.resolve("source");
        Files.createDirectories(source.resolve("Contents"));
        Files.write(source.resolve("mimetype"), "application/hwp+zip".getBytes(StandardCharsets.UTF_8));
        Files.write(source.resolve("Contents/section0.xml"), "<section>한글</section>".getBytes(StandardCharsets.UTF_8));

        Path archive = tempDir.resolve("roundtrip.hwpx");
        HwpxArchive.create(source, archive);

        Path destination = tempDir.resolve("roundtrip");
        HwpxArchive.extract(archive, destination);

        assertEquals("application/hwp+zip", readUtf8(destination.resolve("mimetype")));
        assertEquals("<section>한글</section>", readUtf8(destination.resolve("Contents/section0.xml")));
    }

    @Test
    void writesMimetypeFirstAndUncompressed() throws Exception {
        Path source = tempDir.resolve("source-with-mimetype");
        Files.createDirectories(source.resolve("Contents"));
        Files.write(source.resolve("mimetype"), "application/hwp+zip".getBytes(StandardCharsets.UTF_8));
        Files.write(source.resolve("Contents/section0.xml"), "<section/>".getBytes(StandardCharsets.UTF_8));

        Path archive = tempDir.resolve("mimetype-order.hwpx");
        HwpxArchive.create(source, archive);

        try (ZipFile zip = new ZipFile(archive.toFile())) {
            ZipEntry first = zip.entries().nextElement();
            assertEquals("mimetype", first.getName());
            assertEquals(ZipEntry.STORED, first.getMethod());
        }
    }

    private static void createZip(Path archive, Entry... entries) throws IOException {
        try (OutputStream output = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Entry entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.name));
                zip.write(entry.content.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    private static String readUtf8(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static final class Entry {
        private final String name;
        private final String content;

        private Entry(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }
}
