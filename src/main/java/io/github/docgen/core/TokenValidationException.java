package io.github.docgen.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Indicates invalid token input or unresolved template tokens. */
public class TokenValidationException extends Exception {
    private static final long serialVersionUID = 1L;

    private final Set<String> missingTokens;

    public TokenValidationException(String message) {
        this(message, Collections.<String>emptySet());
    }

    public TokenValidationException(String message, Set<String> missingTokens) {
        super(message);
        this.missingTokens = Collections.unmodifiableSet(new LinkedHashSet<>(missingTokens));
    }

    public Set<String> missingTokens() {
        return missingTokens;
    }
}
