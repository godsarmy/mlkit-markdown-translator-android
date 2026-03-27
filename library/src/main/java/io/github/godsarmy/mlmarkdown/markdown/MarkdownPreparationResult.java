package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;

public final class MarkdownPreparationResult {
    private final ProcessingMode mode;
    private final String markdownForTranslation;
    private final MarkdownTokenStore tokenStore;
    private final TokenizedMarkdownDocument tokenizedDocument;

    public MarkdownPreparationResult(
            ProcessingMode mode,
            String markdownForTranslation,
            MarkdownTokenStore tokenStore,
            TokenizedMarkdownDocument tokenizedDocument) {
        this.mode = mode;
        this.markdownForTranslation = markdownForTranslation;
        this.tokenStore = tokenStore;
        this.tokenizedDocument = tokenizedDocument;
    }

    public ProcessingMode getMode() {
        return mode;
    }

    public String getMarkdownForTranslation() {
        return markdownForTranslation;
    }

    public MarkdownTokenStore getTokenStore() {
        return tokenStore;
    }

    public TokenizedMarkdownDocument getTokenizedDocument() {
        return tokenizedDocument;
    }
}
