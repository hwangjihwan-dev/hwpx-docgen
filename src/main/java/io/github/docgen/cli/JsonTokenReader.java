package io.github.docgen.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.docgen.core.TokenValidationException;

public final class JsonTokenReader {

    private JsonTokenReader() {
    }

    public static Map<String, String> read(Path jsonPath) throws IOException, TokenValidationException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonPath.toFile());
        if (root == null || !root.isObject()) {
            throw new TokenValidationException("Token JSON must be an object");
        }

        Map<String, String> tokens = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (!field.getValue().isTextual()) {
                throw new TokenValidationException("Token value must be a string: " + field.getKey());
            }
            tokens.put(field.getKey(), field.getValue().textValue());
        }
        return Collections.unmodifiableMap(tokens);
    }
}
