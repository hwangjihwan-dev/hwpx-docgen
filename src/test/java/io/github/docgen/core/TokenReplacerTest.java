package io.github.docgen.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

class TokenReplacerTest {

    @Test
    void acceptsAllowedTokenNames() throws Exception {
        Document document = document("<hp:p><hp:run><hp:t>{{title}} {{a_1}} {{a-b}} {{a.b}}</hp:t></hp:run></hp:p>");
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("title", "T");
        tokens.put("a_1", "1");
        tokens.put("a-b", "2");
        tokens.put("a.b", "3");

        ReplacementResult result = new TokenReplacer().replace(document, tokens, false);

        assertEquals(4, result.replacementCount());
    }

    @Test
    void rejectsInvalidTokenNames() throws Exception {
        Document document = document("<hp:p><hp:run><hp:t>{{title}}</hp:t></hp:run></hp:p>");
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("invalid name", "value");

        assertThrows(TokenValidationException.class, () -> new TokenReplacer().replace(document, tokens, false));
    }

    @Test
    void replacesTokenInSingleTextNode() throws Exception {
        Document document = document("<hp:p><hp:run><hp:t>Hello {{title}}</hp:t></hp:run></hp:p>");
        Map<String, String> tokens = singleton("title", "world");

        ReplacementResult result = new TokenReplacer().replace(document, tokens, false);

        assertEquals(1, result.replacementCount());
        assertEquals("Hello world", document.getElementsByTagNameNS("*", "t").item(0).getTextContent());
    }

    @Test
    void replacesTokenAcrossTextNodes() throws Exception {
        Document document = document("<hp:p><hp:run><hp:t>{{ti</hp:t></hp:run>"
                + "<hp:run><hp:t>tle}}</hp:t></hp:run></hp:p>");
        Map<String, String> tokens = singleton("title", "Replaced");

        ReplacementResult result = new TokenReplacer().replace(document, tokens, false);
        NodeList textNodes = document.getElementsByTagNameNS("*", "t");

        assertEquals(1, result.replacementCount());
        assertEquals("Replaced", textNodes.item(0).getTextContent());
        assertEquals("", textNodes.item(1).getTextContent());
    }

    @Test
    void failsOnMissingTokenByDefault() throws Exception {
        Document document = document("<hp:p><hp:run><hp:t>{{missing}}</hp:t></hp:run></hp:p>");

        TokenValidationException exception = assertThrows(TokenValidationException.class,
                () -> new TokenReplacer().replace(document, new LinkedHashMap<>(), false));

        assertTrue(exception.missingTokens().contains("missing"));
        assertEquals("{{missing}}", document.getElementsByTagNameNS("*", "t").item(0).getTextContent());
    }

    @Test
    void leavesMissingTokenWhenAllowed() throws Exception {
        Document document = document("<hp:p><hp:run><hp:t>{{missing}}</hp:t></hp:run></hp:p>");

        ReplacementResult result = new TokenReplacer().replace(document, new LinkedHashMap<>(), true);

        assertEquals(0, result.replacementCount());
        assertTrue(result.missingTokens().contains("missing"));
        assertEquals("{{missing}}", document.getElementsByTagNameNS("*", "t").item(0).getTextContent());
    }

    @Test
    void replacesMultilineValueByCloningParagraphs() throws Exception {
        Document document = document("<hp:subList><hp:p><hp:run><hp:t>{{description}}</hp:t></hp:run>"
                + "<hp:linesegarray/></hp:p></hp:subList>");
        Map<String, String> tokens = singleton("description", "first\nsecond");

        new TokenReplacer().replace(document, tokens, false);

        NodeList paragraphs = document.getElementsByTagNameNS("*", "p");
        assertEquals(2, paragraphs.getLength());
        assertEquals("first", paragraphs.item(0).getTextContent());
        assertEquals("second", paragraphs.item(1).getTextContent());
        assertEquals("1", paragraphs.item(0).getAttributes().getNamedItem("dirty").getNodeValue());
        assertEquals("1", paragraphs.item(1).getAttributes().getNamedItem("dirty").getNodeValue());
        assertEquals(0, document.getElementsByTagNameNS("*", "linesegarray").getLength());
    }

    private static Document document(String body) throws Exception {
        DocumentBuilder builder = XmlSupport.newSecureDocumentBuilder();
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<hp:section xmlns:hp=\"http://www.hancom.co.kr/hwpml/2011/paragraph\">"
                + body + "</hp:section>";
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private static Map<String, String> singleton(String key, String value) {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put(key, value);
        return tokens;
    }
}
