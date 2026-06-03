package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;

public final class MarkdownPreparationResult {
    private final ProcessingMode mode;
    private final String markdownForTranslation;
    private final TokenizedMarkdownDocument tokenizedDocument;

    public MarkdownPreparationResult(
            ProcessingMode mode,
            String markdownForTranslation,
            TokenizedMarkdownDocument tokenizedDocument) {
        this.mode = mode;
        this.markdownForTranslation = markdownForTranslation;
        this.tokenizedDocument = tokenizedDocument;
    }

    public ProcessingMode getMode() {
        return mode;
    }

    public String getMarkdownForTranslation() {
        return markdownForTranslation;
    }

    public TokenizedMarkdownDocument getTokenizedDocument() {
        return tokenizedDocument;
    }
}
