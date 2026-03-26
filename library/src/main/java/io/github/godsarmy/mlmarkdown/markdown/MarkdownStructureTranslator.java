package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;

public class MarkdownStructureTranslator {
    private final TranslationEngine translationEngine;

    public MarkdownStructureTranslator(TranslationEngine translationEngine) {
        this.translationEngine = translationEngine;
    }

    public void translate(String markdown, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
        translationEngine.translate(markdown, sourceLanguage, targetLanguage, callback);
    }
}
