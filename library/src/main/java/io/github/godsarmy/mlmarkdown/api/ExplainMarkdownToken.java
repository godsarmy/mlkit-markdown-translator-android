package io.github.godsarmy.mlmarkdown.api;

import androidx.annotation.Nullable;
import io.github.godsarmy.mlmarkdown.markdown.MarkdownTokenType;

public final class ExplainMarkdownToken {
    private final MarkdownTokenType type;
    @Nullable private final String tokenId;
    private final String value;
    private final int startOffset;
    private final int endOffset;

    public ExplainMarkdownToken(
            MarkdownTokenType type,
            @Nullable String tokenId,
            String value,
            int startOffset,
            int endOffset) {
        this.type = type;
        this.tokenId = tokenId;
        this.value = value;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public MarkdownTokenType getType() {
        return type;
    }

    @Nullable
    public String getTokenId() {
        return tokenId;
    }

    public String getValue() {
        return value;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }
}
