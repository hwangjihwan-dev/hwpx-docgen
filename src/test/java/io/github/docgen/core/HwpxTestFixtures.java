package io.github.docgen.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class HwpxTestFixtures {

    private HwpxTestFixtures() {
    }

    static Path createMinimalHwpx(Path directory, String sectionXml) throws IOException {
        Path archive = directory.resolve("fixture.hwpx");
        try (OutputStream output = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            write(zip, "mimetype", "application/hwp+zip");
            write(zip, "Contents/section0.xml", sectionXml);
        }
        return archive;
    }

    static Path createHwpxWithoutContents(Path directory) throws IOException {
        Path archive = directory.resolve("invalid.hwpx");
        try (OutputStream output = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            write(zip, "mimetype", "application/hwp+zip");
        }
        return archive;
    }

    static String section(String text) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<hp:section xmlns:hp=\"http://www.hancom.co.kr/hwpml/2011/paragraph\">"
                + "<hp:p><hp:run><hp:t>" + text + "</hp:t></hp:run></hp:p>"
                + "</hp:section>";
    }

    private static void write(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
