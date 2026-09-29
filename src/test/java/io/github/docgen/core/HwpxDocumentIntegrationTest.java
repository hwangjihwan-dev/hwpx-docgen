package io.github.docgen.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

class HwpxDocumentIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void rendersTokensIntoHwpx() throws Exception {
        Path template = HwpxTestFixtures.createMinimalHwpx(tempDir,
                HwpxTestFixtures.section("Title: {{title}} / Author: {{author}}"));
        Path output = tempDir.resolve("result.hwpx");
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("title", "Public docgen");
        tokens.put("author", "Example author");

        HwpxDocument document = HwpxDocument.open(template);
        try {
            ReplacementResult result = document.replaceTokens(tokens, false);
            assertEquals(2, result.replacementCount());
            document.write(output, false);
        } finally {
            document.close();
        }

        HwpxDocument rendered = HwpxDocument.open(output);
        try {
            String text = sectionText(rendered.sectionFiles().get(0));
            assertEquals("Title: Public docgen / Author: Example author", text);
            assertFalse(text.contains("{{"));
        } finally {
            rendered.close();
        }
    }

    @Test
    void roundTripsMultilineOutput() throws Exception {
        Path template = HwpxTestFixtures.createMinimalHwpx(tempDir,
                HwpxTestFixtures.section("Description: {{description}}"));
        Path output = tempDir.resolve("multiline.hwpx");
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("description", "first\nsecond");

        HwpxDocument document = HwpxDocument.open(template);
        try {
            document.replaceTokens(tokens, false);
            document.write(output, false);
        } finally {
            document.close();
        }

        HwpxDocument rendered = HwpxDocument.open(output);
        try {
            assertEquals(1, rendered.sectionFiles().size());
            List<String> paragraphs = sectionParagraphTexts(rendered.sectionFiles().get(0));
            assertEquals(2, paragraphs.size());
            assertEquals("Description: first", paragraphs.get(0));
            assertEquals("second", paragraphs.get(1));
        } finally {
            rendered.close();
        }
    }

    private static String sectionText(Path section) throws Exception {
        DocumentBuilder builder = XmlSupport.newSecureDocumentBuilder();
        try (InputStream input = Files.newInputStream(section)) {
            Document document = builder.parse(input);
            return document.getDocumentElement().getTextContent();
        }
    }

    private static List<String> sectionParagraphTexts(Path section) throws Exception {
        DocumentBuilder builder = XmlSupport.newSecureDocumentBuilder();
        try (InputStream input = Files.newInputStream(section)) {
            Document document = builder.parse(input);
            NodeList paragraphs = document.getElementsByTagNameNS("*", "p");
            List<String> texts = new ArrayList<>();
            for (int i = 0; i < paragraphs.getLength(); i++) {
                texts.add(paragraphs.item(i).getTextContent());
            }
            return texts;
        }
    }
}
