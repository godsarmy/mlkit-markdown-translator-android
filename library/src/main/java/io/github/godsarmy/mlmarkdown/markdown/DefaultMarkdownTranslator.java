package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.api.MarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;

public class DefaultMarkdownTranslator implements MarkdownTranslator {
    private final MarkdownPreprocessor preprocessor;
    private final MarkdownProtectionPipeline protectionPipeline;
    private final MarkdownStructureTranslator structureTranslator;
    private final MarkdownRestorer restorer;

    public DefaultMarkdownTranslator(TranslationEngine translationEngine) {
        this.preprocessor = new MarkdownPreprocessor();
        this.protectionPipeline = new MarkdownProtectionPipeline();
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
        String normalized = preprocessor.normalizeLineEndings(markdown);
        MarkdownTokenStore tokenStore = new MarkdownTokenStore();
        String protectedMarkdown = protectionPipeline.protect(normalized, tokenStore);
        structureTranslator.translate(
                protectedMarkdown,
                sourceLanguage,
                targetLanguage,
                new TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        callback.onSuccess(restorer.restore(translatedText, tokenStore));
                    }

                    @Override
                    public void onFailure(Exception error) {
                        callback.onFailure(error);
                    }
                }
        );
    }
}
