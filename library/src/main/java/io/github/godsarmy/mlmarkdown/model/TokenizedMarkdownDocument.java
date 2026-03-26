package io.github.godsarmy.mlmarkdown.model;

import io.github.godsarmy.mlmarkdown.markdown.MarkdownToken;
import io.github.godsarmy.mlmarkdown.markdown.MarkdownTokenType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public String reconstructWithTranslations(Map<String, String> translationByTokenId) {
        StringBuilder builder = new StringBuilder();
        for (MarkdownToken token : tokens) {
            if (token.getType() == MarkdownTokenType.TRANSLATABLE && token.getTokenId() != null) {
                String translated = translationByTokenId.get(token.getTokenId());
                builder.append(translated != null ? translated : token.getValue());
            } else {
                builder.append(token.getValue());
            }
        }
        return builder.toString();
    }

    public Map<String, String> translatableTokenMap() {
        Map<String, String> map = new HashMap<>();
        for (MarkdownToken token : tokens) {
            if (token.getType() == MarkdownTokenType.TRANSLATABLE && token.getTokenId() != null) {
                map.put(token.getTokenId(), token.getValue());
            }
        }
        return map;
    }
}
