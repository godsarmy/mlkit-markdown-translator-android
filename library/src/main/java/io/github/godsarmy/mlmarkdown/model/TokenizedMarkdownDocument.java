package io.github.godsarmy.mlmarkdown.model;

import io.github.godsarmy.mlmarkdown.markdown.MarkdownToken;

import java.util.Collections;
import java.util.List;

public final class TokenizedMarkdownDocument {
    private final String source;
    private final List<MarkdownToken> tokens;

    public TokenizedMarkdownDocument(String source, List<MarkdownToken> tokens) {
        this.source = source;
        this.tokens = List.copyOf(tokens);
    }

    public String getSource() {
        return source;
    }

    public List<MarkdownToken> getTokens() {
        return Collections.unmodifiableList(tokens);
    }

    public String reconstruct() {
        StringBuilder builder = new StringBuilder();
        for (MarkdownToken token : tokens) {
            builder.append(token.getValue());
        }
        return builder.toString();
    }
}
