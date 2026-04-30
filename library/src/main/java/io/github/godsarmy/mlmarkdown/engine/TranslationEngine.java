package io.github.godsarmy.mlmarkdown.engine;

import io.github.godsarmy.mlmarkdown.api.TranslationCallback;

public interface TranslationEngine {
    void translate(
            String text,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback);

    default void translate(
            String text,
            String sourceLanguage,
            String targetLanguage,
            long timeoutMs,
            TranslationCallback callback) {
        translate(text, sourceLanguage, targetLanguage, callback);
    }
}
