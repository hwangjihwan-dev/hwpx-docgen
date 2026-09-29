package io.github.docgen.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/** Lifecycle for an extracted HWPX document. */
public final class HwpxDocument implements AutoCloseable {

    private final Path workspaceDirectory;
    private final List<Path> sectionFiles;
    private boolean closed;

    private HwpxDocument(Path workspaceDirectory, List<Path> sectionFiles) {
        this.workspaceDirectory = workspaceDirectory;
        this.sectionFiles = Collections.unmodifiableList(new ArrayList<>(sectionFiles));
    }

    public static HwpxDocument open(Path templatePath) throws IOException, HwpxFormatException {
        if (!Files.isRegularFile(templatePath)) {
            throw new IOException("HWPX template does not exist: " + templatePath);
        }

        Path workspace = Files.createTempDirectory("docgen-hwpx-");
        try {
            HwpxArchive.extract(templatePath, workspace);
            Path contents = workspace.resolve("Contents");
            if (!Files.isDirectory(contents)) {
                throw new HwpxFormatException("HWPX Contents directory is missing: " + contents);
            }

            List<Path> sections = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(contents, "section*.xml")) {
                for (Path section : stream) {
                    if (Files.isRegularFile(section)) {
                        sections.add(section);
                    }
                }
            }
            sections.sort(Comparator.comparing(path -> path.getFileName().toString()));
            if (sections.isEmpty()) {
                throw new HwpxFormatException("HWPX has no Contents/section*.xml files");
            }
            return new HwpxDocument(workspace, sections);
        } catch (IOException | RuntimeException exception) {
            deleteRecursively(workspace);
            throw exception;
        }
    }

    public List<Path> sectionFiles() {
        ensureOpen();
        return sectionFiles;
    }

    public ReplacementResult replaceTokens(Map<String, String> tokens, boolean allowMissing)
            throws IOException, TokenValidationException {
        ensureOpen();
        TokenReplacer replacer = new TokenReplacer();
        int replacementCount = 0;
        Set<String> missingTokens = new LinkedHashSet<>();

        for (Path section : sectionFiles) {
            Document document = parse(section);
            ReplacementResult result = replacer.replace(document, tokens, allowMissing);
            writeXml(document, section);
            replacementCount += result.replacementCount();
            missingTokens.addAll(result.missingTokens());
        }
        return new ReplacementResult(replacementCount, missingTokens);
    }

    public void write(Path outputPath, boolean overwrite) throws IOException {
        ensureOpen();
        if (Files.exists(outputPath) && !overwrite) {
            throw new IOException("Output file already exists: " + outputPath);
        }

        Path absoluteOutput = outputPath.toAbsolutePath().normalize();
        Path parent = absoluteOutput.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporaryOutput = Files.createTempFile(parent, ".docgen-", ".hwpx");
        try {
            HwpxArchive.create(workspaceDirectory, temporaryOutput);
            if (overwrite) {
                move(temporaryOutput, absoluteOutput, StandardCopyOption.REPLACE_EXISTING);
            } else {
                move(temporaryOutput, absoluteOutput);
            }
        } finally {
            Files.deleteIfExists(temporaryOutput);
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            deleteRecursively(workspaceDirectory);
        }
    }

    private static void move(Path source, Path target, StandardCopyOption... options) throws IOException {
        try {
            StandardCopyOption[] atomicOptions = new StandardCopyOption[options.length + 1];
            System.arraycopy(options, 0, atomicOptions, 0, options.length);
            atomicOptions[options.length] = StandardCopyOption.ATOMIC_MOVE;
            Files.move(source, target, atomicOptions);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, options);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("HWPX document is already closed");
        }
    }

    private static Document parse(Path section) throws IOException, HwpxFormatException {
        try {
            DocumentBuilder builder = XmlSupport.newSecureDocumentBuilder();
            return builder.parse(section.toFile());
        } catch (ParserConfigurationException | SAXException exception) {
            throw new HwpxFormatException("Invalid HWPX section XML: " + section, exception);
        }
    }

    private static void writeXml(Document document, Path section) throws IOException {
        try {
            Transformer transformer = XmlSupport.newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(section.toFile()));
        } catch (TransformerException exception) {
            throw new IOException("Could not write HWPX section XML: " + section, exception);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            try {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                });
            } catch (UncheckedIOException exception) {
                throw exception.getCause();
            }
        }
    }
}
