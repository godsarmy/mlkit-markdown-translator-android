package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;
import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;

import java.util.LinkedHashMap;
import java.util.Map;

public class MarkdownStructureTranslator {
    private final TranslationEngine translationEngine;

    public MarkdownStructureTranslator(TranslationEngine translationEngine) {
        this.translationEngine = translationEngine;
    }

    public void translate(String markdown, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
        translationEngine.translate(markdown, sourceLanguage, targetLanguage, callback);
    }

    public void translate(
            TokenizedMarkdownDocument tokenizedDocument,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback
    ) {
        Map<String, String> translatableTokens = new LinkedHashMap<>(tokenizedDocument.translatableTokenMap());
        if (translatableTokens.isEmpty()) {
            callback.onSuccess(tokenizedDocument.reconstruct());
            return;
        }

        translateNext(
                tokenizedDocument,
                translatableTokens.entrySet().iterator(),
                new LinkedHashMap<>(),
                sourceLanguage,
                targetLanguage,
                callback
        );
    }

    private void translateNext(
            TokenizedMarkdownDocument tokenizedDocument,
            java.util.Iterator<Map.Entry<String, String>> iterator,
            Map<String, String> translations,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback
    ) {
        if (!iterator.hasNext()) {
            callback.onSuccess(tokenizedDocument.reconstructWithTranslations(translations));
            return;
        }

        Map.Entry<String, String> entry = iterator.next();
        translationEngine.translate(
                entry.getValue(),
                sourceLanguage,
                targetLanguage,
                new TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        translations.put(entry.getKey(), translatedText);
                        translateNext(
                                tokenizedDocument,
                                iterator,
                                translations,
                                sourceLanguage,
                                targetLanguage,
                                callback
                        );
                    }

                    @Override
                    public void onFailure(Exception error) {
                        callback.onFailure(error);
                    }
                }
        );
    }
}
