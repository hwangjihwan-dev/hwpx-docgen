package io.github.docgen.core;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class XmlSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void doesNotResolveExternalEntities() throws Exception {
        Path secret = tempDir.resolve("secret.txt");
        Files.write(secret, "sensitive-value".getBytes(StandardCharsets.UTF_8));
        String xml = "<?xml version=\"1.0\"?>"
                + "<!DOCTYPE data [<!ENTITY secret SYSTEM \"" + secret.toUri() + "\">]>"
                + "<data>&secret;</data>";

        DocumentBuilder builder = XmlSupport.newSecureDocumentBuilder();
        try {
            String text = builder.parse(new InputSource(new StringReader(xml)))
                    .getDocumentElement().getTextContent();
            assertNotEquals("sensitive-value", text);
            assertTrue(text == null || text.trim().isEmpty());
        } catch (SAXException expected) {
            // Rejecting the document is also a valid secure outcome.
        }
    }
}
