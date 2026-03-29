package io.github.godsarmy.mlmarkdown;

import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownResult;
import io.github.godsarmy.mlmarkdown.api.MarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.MlKitTranslationEngine;
import io.github.godsarmy.mlmarkdown.markdown.DefaultMarkdownTranslator;
import java.io.Closeable;

public final class MlKitMarkdownTranslator implements Closeable {
    private final MarkdownTranslator markdownTranslator;
    private final Closeable closeableResource;

    public MlKitMarkdownTranslator() {
        this(MarkdownTranslationOptions.defaults());
    }

    public MlKitMarkdownTranslator(MarkdownTranslationOptions options) {
        this(new MlKitTranslationEngine(), options);
    }

    private MlKitMarkdownTranslator(
            MlKitTranslationEngine translationEngine, MarkdownTranslationOptions options) {
        this(new DefaultMarkdownTranslator(translationEngine, options), translationEngine);
    }

    MlKitMarkdownTranslator(MarkdownTranslator markdownTranslator, Closeable closeableResource) {
        this.markdownTranslator = markdownTranslator;
        this.closeableResource = closeableResource;
    }

    public void translateMarkdown(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback) {
        markdownTranslator.translateMarkdown(markdown, sourceLanguage, targetLanguage, callback);
    }

    public ExplainMarkdownResult explainMarkdown(String markdown) {
        return markdownTranslator.explainMarkdown(markdown);
    }

    @Override
    public void close() {
        try {
            closeableResource.close();
        } catch (Exception ignored) {
            // No-op for the simple facade close path.
        }
    }
}
