package io.github.docgen.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.zip.CRC32;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** ZIP container operations used by HWPX documents. */
public final class HwpxArchive {

    private HwpxArchive() {
    }

    public static void extract(Path archivePath, Path destinationDirectory) throws IOException {
        requireRegularFile(archivePath, "HWPX archive");
        Files.createDirectories(destinationDirectory);

        Path root = destinationDirectory.toAbsolutePath().normalize();
        try (InputStream input = Files.newInputStream(archivePath);
             ZipInputStream zip = new ZipInputStream(new BufferedInputStream(input))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                Path target = safeEntryTarget(root, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    try (OutputStream output = Files.newOutputStream(target)) {
                        int count;
                        while ((count = zip.read(buffer)) != -1) {
                            if (count > 0) {
                                output.write(buffer, 0, count);
                            }
                        }
                    }
                }
                zip.closeEntry();
            }
        }
    }

    public static void create(Path sourceDirectory, Path outputArchive) throws IOException {
        if (!Files.isDirectory(sourceDirectory)) {
            throw new IOException("Source directory does not exist: " + sourceDirectory);
        }
        Path parent = outputArchive.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (OutputStream output = Files.newOutputStream(outputArchive);
             ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(output));
             Stream<Path> paths = Files.walk(sourceDirectory)) {
            try {
                paths.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(path -> entryName(sourceDirectory, path),
                                HwpxArchive::compareEntryNames))
                        .forEach(path -> writeEntry(sourceDirectory, path, zip));
            } catch (UncheckedIOException exception) {
                throw exception.getCause();
            }
        }
    }

    private static void writeEntry(Path sourceDirectory, Path path, ZipOutputStream zip) {
        String name = entryName(sourceDirectory, path);
        if ("mimetype".equals(name)) {
            writeStoredMimetype(path, name, zip);
            return;
        }
        try (InputStream input = Files.newInputStream(path)) {
            zip.putNextEntry(new ZipEntry(name));
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count > 0) {
                    zip.write(buffer, 0, count);
                }
            }
            zip.closeEntry();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void writeStoredMimetype(Path path, String name, ZipOutputStream zip) {
        try {
            byte[] content = Files.readAllBytes(path);
            CRC32 checksum = new CRC32();
            checksum.update(content);
            ZipEntry entry = new ZipEntry(name);
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(content.length);
            entry.setCrc(checksum.getValue());
            zip.putNextEntry(entry);
            zip.write(content);
            zip.closeEntry();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String entryName(Path sourceDirectory, Path path) {
        return sourceDirectory.relativize(path).toString().replace('\\', '/');
    }

    private static int compareEntryNames(String left, String right) {
        if ("mimetype".equals(left)) {
            return "mimetype".equals(right) ? 0 : -1;
        }
        if ("mimetype".equals(right)) {
            return 1;
        }
        return left.compareTo(right);
    }

    private static Path safeEntryTarget(Path root, String entryName) throws IOException {
        if (entryName == null || entryName.trim().isEmpty()) {
            throw new IOException("ZIP entry has no name");
        }
        Path target = root.resolve(Paths.get(entryName)).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Blocked unsafe ZIP entry: " + entryName);
        }
        return target;
    }

    private static void requireRegularFile(Path path, String label) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException(label + " does not exist: " + path);
        }
    }
}
