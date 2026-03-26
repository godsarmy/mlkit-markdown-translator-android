package io.github.godsarmy.mlmarkdown.markdown;

public final class MarkdownToken {
    private final MarkdownTokenType type;
    private final String value;
    private final int startOffset;
    private final int endOffset;

    public MarkdownToken(String value) {
        this(MarkdownTokenType.STRUCTURAL, value, -1, -1);
    }

    public MarkdownToken(MarkdownTokenType type, String value, int startOffset, int endOffset) {
        this.type = type;
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

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }
}
