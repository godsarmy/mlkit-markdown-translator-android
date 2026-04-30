package io.github.godsarmy.mlmarkdown.api;

public interface MarkdownTranslator {
    void translateMarkdown(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback);

    default void translateMarkdown(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            long timeoutMs,
            TranslationCallback callback) {
        translateMarkdown(markdown, sourceLanguage, targetLanguage, callback);
    }

    ExplainMarkdownResult explainMarkdown(String markdown);
}
