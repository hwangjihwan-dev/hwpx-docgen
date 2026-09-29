package io.github.docgen.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Replaces {{token}} values in HWPX paragraph text. */
public final class TokenReplacer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{([A-Za-z0-9_.-]+)\\}\\}");
    private static final Pattern TOKEN_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_.-]+");

    public ReplacementResult replace(Document document, Map<String, String> tokens, boolean allowMissing)
            throws TokenValidationException {
        if (document == null) {
            throw new TokenValidationException("Document must not be null");
        }
        if (tokens == null) {
            throw new TokenValidationException("Tokens must not be null");
        }
        validateTokenMap(tokens);

        List<Element> paragraphs = elementsByLocalName(document, "p");
        Set<String> missing = new LinkedHashSet<>();
        for (Element paragraph : paragraphs) {
            Matcher matcher = TOKEN_PATTERN.matcher(textOf(paragraph));
            while (matcher.find()) {
                String name = matcher.group(1);
                if (!tokens.containsKey(name)) {
                    missing.add(name);
                }
            }
        }
        if (!allowMissing && !missing.isEmpty()) {
            throw new TokenValidationException("Missing tokens: " + missing, missing);
        }

        int replacementCount = 0;
        for (Element paragraph : paragraphs) {
            String original = textOf(paragraph);
            Matcher matcher = TOKEN_PATTERN.matcher(original);
            StringBuilder replaced = new StringBuilder();
            int lastEnd = 0;
            boolean changed = false;
            while (matcher.find()) {
                replaced.append(original, lastEnd, matcher.start());
                String name = matcher.group(1);
                if (tokens.containsKey(name)) {
                    replaced.append(tokens.get(name));
                    replacementCount++;
                    changed = true;
                } else {
                    replaced.append(matcher.group());
                }
                lastEnd = matcher.end();
            }
            if (!changed) {
                continue;
            }
            replaced.append(original, lastEnd, original.length());
            String value = replaced.toString();
            if (containsLineBreak(value)) {
                replaceParagraphWithLines(paragraph, value);
            } else {
                writeParagraphText(paragraph, value);
            }
        }
        if (replacementCount > 0) {
            // Hancom validates paragraph layout caches across the section. Recompute them all
            // after a text edit instead of leaving stale caches in untouched paragraphs.
            removeLineSegments(document);
        }
        return new ReplacementResult(replacementCount, missing);
    }

    private static void validateTokenMap(Map<String, String> tokens) throws TokenValidationException {
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            String name = entry.getKey();
            if (name == null || !TOKEN_NAME_PATTERN.matcher(name).matches()) {
                throw new TokenValidationException("Invalid token name: " + name);
            }
            if (entry.getValue() == null) {
                throw new TokenValidationException("Token value must not be null: " + name);
            }
        }
    }

    private static List<Element> elementsByLocalName(Node root, String localName) {
        List<Element> elements = new ArrayList<>();
        if (root instanceof Element && localName.equals(root.getLocalName())) {
            elements.add((Element) root);
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            elements.addAll(elementsByLocalName(children.item(i), localName));
        }
        return elements;
    }

    private static List<Element> textElements(Element paragraph) {
        return elementsByLocalName(paragraph, "t");
    }

    private static String textOf(Element paragraph) {
        StringBuilder text = new StringBuilder();
        for (Element node : textElements(paragraph)) {
            text.append(node.getTextContent());
        }
        return text.toString();
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    }

    private static void writeParagraphText(Element paragraph, String value) {
        List<Element> textNodes = textElements(paragraph);
        if (textNodes.isEmpty()) {
            return;
        }
        textNodes.get(0).setTextContent(value);
        for (int i = 1; i < textNodes.size(); i++) {
            textNodes.get(i).setTextContent("");
        }
        markDirtyAndRemoveLineSegments(paragraph);
    }

    private static void replaceParagraphWithLines(Element paragraph, String value) {
        String[] lines = value.split("\\r\\n|\\r|\\n", -1);
        markDirtyAndRemoveLineSegments(paragraph);
        writeLine(paragraph, lines[0]);

        Node parent = paragraph.getParentNode();
        Node insertAfter = paragraph;
        for (int i = 1; i < lines.length; i++) {
            Node copy = paragraph.cloneNode(true);
            if (copy instanceof Element) {
                ((Element) copy).setAttribute("dirty", "1");
            }
            removeLineSegments(copy);
            writeLine(copy, lines[i]);

            Node next = insertAfter.getNextSibling();
            if (next == null) {
                parent.appendChild(copy);
            } else {
                parent.insertBefore(copy, next);
            }
            insertAfter = copy;
        }
    }

    private static void writeLine(Node paragraph, String value) {
        List<Element> textNodes = elementsByLocalName(paragraph, "t");
        if (textNodes.isEmpty()) {
            return;
        }
        for (Element textNode : textNodes) {
            textNode.setTextContent("");
        }
        textNodes.get(0).setTextContent(value.isEmpty() ? " " : value);
    }

    private static void markDirtyAndRemoveLineSegments(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof Element) {
                String localName = current.getLocalName();
                if ("p".equals(localName) || "subList".equals(localName) || "tc".equals(localName)) {
                    ((Element) current).setAttribute("dirty", "1");
                }
            }
            current = current.getParentNode();
        }
        removeLineSegments(node);
    }

    private static void removeLineSegments(Node root) {
        List<Node> segments = nodesByLocalName(root, "linesegarray");
        for (Node segment : segments) {
            Node parent = segment.getParentNode();
            if (parent != null) {
                parent.removeChild(segment);
            }
        }
    }

    private static List<Node> nodesByLocalName(Node root, String localName) {
        List<Node> nodes = new ArrayList<>();
        if (localName.equals(root.getLocalName())) {
            nodes.add(root);
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            nodes.addAll(nodesByLocalName(children.item(i), localName));
        }
        return nodes;
    }
}
