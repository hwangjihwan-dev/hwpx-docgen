package io.github.docgen.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Summary of a token replacement pass. */
public final class ReplacementResult {
    private final int replacementCount;
    private final Set<String> missingTokens;

    public ReplacementResult(int replacementCount, Set<String> missingTokens) {
        this.replacementCount = replacementCount;
        this.missingTokens = Collections.unmodifiableSet(new LinkedHashSet<>(missingTokens));
    }

    public int replacementCount() {
        return replacementCount;
    }

    public Set<String> missingTokens() {
        return missingTokens;
    }
}
