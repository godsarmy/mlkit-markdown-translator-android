package io.github.godsarmy.mlmarkdown.markdown;

public final class MarkdownToken {
    private final MarkdownTokenType type;
    private final String tokenId;
    private final String value;
    private final int startOffset;
    private final int endOffset;

    public MarkdownToken(String value) {
        this(MarkdownTokenType.STRUCTURAL, null, value, -1, -1);
    }

    public MarkdownToken(MarkdownTokenType type, String value, int startOffset, int endOffset) {
        this(type, null, value, startOffset, endOffset);
    }

    public MarkdownToken(
            MarkdownTokenType type,
            String tokenId,
            String value,
            int startOffset,
            int endOffset
    ) {
        this.type = type;
        this.tokenId = tokenId;
        this.value = value;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public MarkdownTokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getTokenId() {
        return tokenId;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public MarkdownToken withValue(String newValue) {
        return new MarkdownToken(type, tokenId, newValue, startOffset, endOffset);
    }

    public MarkdownToken withTokenId(String newTokenId) {
        return new MarkdownToken(type, newTokenId, value, startOffset, endOffset);
    }
}
