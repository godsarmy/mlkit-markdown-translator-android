package io.github.godsarmy.mlmarkdown.model;

public final class ProtectedSegment {
    private final String token;
    private final String originalText;

    public ProtectedSegment(String token, String originalText) {
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
