package io.github.godsarmy.mlmarkdown.api;

public final class ExplainProtectedSegment {
    private final String token;
    private final String originalText;

    public ExplainProtectedSegment(String token, String originalText) {
        this.token = token;
        this.originalText = originalText;
    }

    public String getToken() {
        return token;
    }

    public String getOriginalText() {
        return originalText;
    }
}
