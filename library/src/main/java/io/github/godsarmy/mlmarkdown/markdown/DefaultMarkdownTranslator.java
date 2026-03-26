package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.api.MarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;

public class DefaultMarkdownTranslator implements MarkdownTranslator {
    private final HybridMarkdownPreparationService preparationService;
    private final MarkdownStructureTranslator structureTranslator;
    private final MarkdownRestorer restorer;

    public DefaultMarkdownTranslator(TranslationEngine translationEngine) {
        this.preparationService = new HybridMarkdownPreparationService();
        this.structureTranslator = new MarkdownStructureTranslator(translationEngine);
        this.restorer = new MarkdownRestorer();
    }

    @Override
    public void translateMarkdown(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback
    ) {
        MarkdownPreparationResult preparationResult = preparationService.prepare(markdown);
        structureTranslator.translate(
                preparationResult.getMarkdownForTranslation(),
                sourceLanguage,
                targetLanguage,
                new TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        if (preparationResult.getMode() == ProcessingMode.REGEX_FALLBACK) {
                            callback.onSuccess(restorer.restore(translatedText, preparationResult.getTokenStore()));
                            return;
                        }
                        callback.onSuccess(translatedText);
                    }

                    @Override
                    public void onFailure(Exception error) {
                        callback.onFailure(error);
                    }
                }
        );
    }
}
