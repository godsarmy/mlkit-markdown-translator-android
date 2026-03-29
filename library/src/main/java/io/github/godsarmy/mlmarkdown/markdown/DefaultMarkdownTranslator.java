package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownChunk;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownResult;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownToken;
import io.github.godsarmy.mlmarkdown.api.ExplainProtectedSegment;
import io.github.godsarmy.mlmarkdown.api.MarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationTimingListener;
import io.github.godsarmy.mlmarkdown.api.TranslationTimingReport;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;
import io.github.godsarmy.mlmarkdown.model.ProtectedSegment;
import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;
import java.util.ArrayList;
import java.util.List;

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
        int totalChunkCount = totalChunkCount(preparationResult);

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
                                    totalChunkCount,
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
                                    totalChunkCount,
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
                                    totalChunkCount,
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
                                totalChunkCount,
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
                                totalChunkCount,
                                false,
                                error);
                        callback.onFailure(error);
                    }
                });
    }

    @Override
    public ExplainMarkdownResult explainMarkdown(String markdown) {
        MarkdownPreparationResult preparationResult = preparationService.prepare(markdown);
        List<ExplainMarkdownChunk> chunks = new ArrayList<>();
        List<ExplainMarkdownToken> tokens = new ArrayList<>();
        List<ExplainProtectedSegment> protectedSegments = new ArrayList<>();

        if (preparationResult.getTokenizedDocument() != null) {
            TokenizedMarkdownDocument tokenizedDocument = preparationResult.getTokenizedDocument();
            chunks = explainChunks(tokenizedDocument);
            tokens = explainTokens(tokenizedDocument);
        }

        if (preparationResult.getTokenStore() != null) {
            for (ProtectedSegment protectedSegment : preparationResult.getTokenStore().getAll()) {
                protectedSegments.add(
                        new ExplainProtectedSegment(
                                protectedSegment.getToken(), protectedSegment.getOriginalText()));
            }
        }

        return new ExplainMarkdownResult(
                preparationResult.getMode(),
                preparationResult.getMarkdownForTranslation(),
                tokens,
                chunks,
                protectedSegments);
    }

    private void notifyTiming(
            ProcessingMode processingMode,
            long preparationDurationMs,
            long translationDurationMs,
            long restorationDurationMs,
            long totalStartNanos,
            int totalTokenCount,
            int totalChunkCount,
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
                        totalChunkCount,
                        successful,
                        error));
    }

    private int totalChunkCount(MarkdownPreparationResult preparationResult) {
        if (preparationResult.getMode() == ProcessingMode.AST_TOKEN_STREAM
                && preparationResult.getTokenizedDocument() != null) {
            return structureTranslator
                    .chunkTranslatableTokens(preparationResult.getTokenizedDocument())
                    .size();
        }

        if (preparationResult.getMode() == ProcessingMode.REGEX_FALLBACK) {
            return 1;
        }

        return 0;
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

    private List<ExplainMarkdownChunk> explainChunks(TokenizedMarkdownDocument tokenizedDocument) {
        List<MarkdownStructureTranslator.TranslationChunk> translationChunks =
                structureTranslator.chunkTranslatableTokens(tokenizedDocument);
        List<ExplainMarkdownChunk> chunks = new ArrayList<>();
        for (int i = 0; i < translationChunks.size(); i++) {
            MarkdownStructureTranslator.TranslationChunk chunk = translationChunks.get(i);
            chunks.add(
                    new ExplainMarkdownChunk(
                            i, chunk.getText(), chunk.getTokenIds(), chunk.getTokenValues()));
        }
        return chunks;
    }

    private static List<ExplainMarkdownToken> explainTokens(
            TokenizedMarkdownDocument tokenizedDocument) {
        List<ExplainMarkdownToken> tokens = new ArrayList<>();
        for (MarkdownToken token : tokenizedDocument.getTokens()) {
            tokens.add(
                    new ExplainMarkdownToken(
                            token.getType(),
                            token.getTokenId(),
                            token.getValue(),
                            token.getStartOffset(),
                            token.getEndOffset()));
        }
        return tokens;
    }

    interface NanoTimeProvider {
        long nowNanos();
    }
}
