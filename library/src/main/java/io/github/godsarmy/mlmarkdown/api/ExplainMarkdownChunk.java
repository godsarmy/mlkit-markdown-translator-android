package io.github.godsarmy.mlmarkdown.api;

import java.util.List;

public final class ExplainMarkdownChunk {
    private final int index;
    private final String rawText;
    private final List<String> tokenIds;
    private final List<String> tokenValues;

    public ExplainMarkdownChunk(
            int index, String rawText, List<String> tokenIds, List<String> tokenValues) {
        this.index = index;
        this.rawText = rawText;
        this.tokenIds = List.copyOf(tokenIds);
        this.tokenValues = List.copyOf(tokenValues);
    }

    public int getIndex() {
        return index;
    }

    public String getRawText() {
        return rawText;
    }

    public List<String> getTokenIds() {
        return tokenIds;
    }

    public List<String> getTokenValues() {
        return tokenValues;
    }

    public int getPlainTextLength() {
        int totalLength = 0;
        for (String tokenValue : tokenValues) {
            totalLength += tokenValue.length();
        }
        return totalLength;
    }
}
