package io.github.godsarmy.mlmarkdown.markdown;

public final class MarkdownToken {
    private final String value;

    public MarkdownToken(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
