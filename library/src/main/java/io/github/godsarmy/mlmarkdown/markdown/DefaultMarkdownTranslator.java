package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.api.MarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationTimingListener;
import io.github.godsarmy.mlmarkdown.api.TranslationTimingReport;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;

public class DefaultMarkdownTranslator implements MarkdownTranslator {
    private final HybridMarkdownPreparationService preparationService;
    private final MarkdownStructureTranslator structureTranslator;
    private final MarkdownRestorer restorer;
    private final NanoTimeProvider nanoTimeProvider;
    private final TranslationTimingListener translationTimingListener;

    public DefaultMarkdownTranslator(TranslationEngine translationEngine) {
        this(translationEngine, MarkdownTranslationOptions.defaults());
    }

    public DefaultMarkdownTranslator(
            TranslationEngine translationEngine, MarkdownTranslationOptions options) {
        this(translationEngine, options, System::nanoTime);
    }

    DefaultMarkdownTranslator(
            TranslationEngine translationEngine,
            MarkdownTranslationOptions options,
            NanoTimeProvider nanoTimeProvider) {
        this.preparationService = new HybridMarkdownPreparationService(options);
        this.structureTranslator =
                new MarkdownStructureTranslator(
                        translationEngine, options.preserveWhitespaceAroundProtectedSegments());
        this.restorer = new MarkdownRestorer();
        this.nanoTimeProvider = nanoTimeProvider;
        this.translationTimingListener = options.translationTimingListener();
    }

    @Override
    public void translateMarkdown(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback) {
        long totalStartNanos = nanoTimeProvider.nowNanos();
        long preparationStartNanos = nanoTimeProvider.nowNanos();
        MarkdownPreparationResult preparationResult = preparationService.prepare(markdown);
        long preparationDurationMs = toMillis(nanoTimeProvider.nowNanos() - preparationStartNanos);
        int totalTokenCount = totalTokenCount(preparationResult);

        long translationStartNanos = nanoTimeProvider.nowNanos();
        if (preparationResult.getMode() == ProcessingMode.AST_TOKEN_STREAM
                && preparationResult.getTokenizedDocument() != null) {
            structureTranslator.translate(
                    preparationResult.getTokenizedDocument(),
                    sourceLanguage,
                    targetLanguage,
                    new TranslationCallback() {
                        @Override
                        public void onSuccess(String translatedText) {
                            long translationDurationMs =
                                    toMillis(nanoTimeProvider.nowNanos() - translationStartNanos);
                            notifyTiming(
                                    preparationResult.getMode(),
                                    preparationDurationMs,
                                    translationDurationMs,
                                    0,
                                    totalStartNanos,
                                    totalTokenCount,
                                    true,
                                    null);
                            callback.onSuccess(translatedText);
                        }

                        @Override
                        public void onFailure(Exception error) {
                            long translationDurationMs =
                                    toMillis(nanoTimeProvider.nowNanos() - translationStartNanos);
                            notifyTiming(
                                    preparationResult.getMode(),
                                    preparationDurationMs,
                                    translationDurationMs,
                                    0,
                                    totalStartNanos,
                                    totalTokenCount,
                                    false,
                                    error);
                            callback.onFailure(error);
                        }
                    });
            return;
        }

        structureTranslator.translate(
                preparationResult.getMarkdownForTranslation(),
                sourceLanguage,
                targetLanguage,
                new TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        long translationDurationMs =
                                toMillis(nanoTimeProvider.nowNanos() - translationStartNanos);
                        long restorationDurationMs = 0;
                        if (preparationResult.getMode() == ProcessingMode.REGEX_FALLBACK) {
                            long restorationStartNanos = nanoTimeProvider.nowNanos();
                            String restored =
                                    restorer.restore(
                                            translatedText, preparationResult.getTokenStore());
                            restorationDurationMs =
                                    toMillis(nanoTimeProvider.nowNanos() - restorationStartNanos);
                            notifyTiming(
                                    preparationResult.getMode(),
                                    preparationDurationMs,
                                    translationDurationMs,
                                    restorationDurationMs,
                                    totalStartNanos,
                                    totalTokenCount,
                                    true,
                                    null);
                            callback.onSuccess(restored);
                            return;
                        }

                        notifyTiming(
                                preparationResult.getMode(),
                                preparationDurationMs,
                                translationDurationMs,
                                restorationDurationMs,
                                totalStartNanos,
                                totalTokenCount,
                                true,
                                null);
                        callback.onSuccess(translatedText);
                    }

                    @Override
                    public void onFailure(Exception error) {
                        long translationDurationMs =
                                toMillis(nanoTimeProvider.nowNanos() - translationStartNanos);
                        notifyTiming(
                                preparationResult.getMode(),
                                preparationDurationMs,
                                translationDurationMs,
                                0,
                                totalStartNanos,
                                totalTokenCount,
                                false,
                                error);
                        callback.onFailure(error);
                    }
                });
    }

    private void notifyTiming(
            ProcessingMode processingMode,
            long preparationDurationMs,
            long translationDurationMs,
            long restorationDurationMs,
            long totalStartNanos,
            int totalTokenCount,
            boolean successful,
            Exception error) {
        if (translationTimingListener == null) {
            return;
        }
        translationTimingListener.onCompleted(
                new TranslationTimingReport(
                        processingMode,
                        preparationDurationMs,
                        translationDurationMs,
                        restorationDurationMs,
                        toMillis(nanoTimeProvider.nowNanos() - totalStartNanos),
                        totalTokenCount,
                        successful,
                        error));
    }

    private static int totalTokenCount(MarkdownPreparationResult preparationResult) {
        if (preparationResult.getTokenizedDocument() != null) {
            return preparationResult.getTokenizedDocument().getTokens().size();
        }
        if (preparationResult.getTokenStore() != null) {
            return preparationResult.getTokenStore().getAll().size();
        }
        return 0;
    }

    private static long toMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    interface NanoTimeProvider {
        long nowNanos();
    }
}
