package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownChunk;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownResult;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownToken;
import io.github.godsarmy.mlmarkdown.api.MarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationMetricsListener;
import io.github.godsarmy.mlmarkdown.api.TranslationMetricsReport;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;
import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;
import java.util.ArrayList;
import java.util.List;

public class DefaultMarkdownTranslator implements MarkdownTranslator {
    private final HybridMarkdownPreparationService preparationService;
    private final MarkdownStructureTranslator structureTranslator;
    private final NanoTimeProvider nanoTimeProvider;
    private final TranslationMetricsListener translationMetricsListener;

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
        this(
                translationEngine,
                options,
                nanoTimeProvider,
                new HybridMarkdownPreparationService(options));
    }

    DefaultMarkdownTranslator(
            TranslationEngine translationEngine,
            MarkdownTranslationOptions options,
            NanoTimeProvider nanoTimeProvider,
            HybridMarkdownPreparationService preparationService) {
        this.preparationService = preparationService;
        this.structureTranslator = new MarkdownStructureTranslator(translationEngine, options);
        this.nanoTimeProvider = nanoTimeProvider;
        this.translationMetricsListener = options.translationMetricsListener();
    }

    @Override
    public void translateMarkdown(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback) {
        translateMarkdown(markdown, sourceLanguage, targetLanguage, 0, callback);
    }

    @Override
    public void translateMarkdown(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            long timeoutMs,
            TranslationCallback callback) {
        long totalStartNanos = nanoTimeProvider.nowNanos();
        long preparationStartNanos = nanoTimeProvider.nowNanos();
        MarkdownPreparationResult preparationResult;
        try {
            preparationResult = preparationService.prepare(markdown);
        } catch (RuntimeException error) {
            long preparationDurationMs =
                    toMillis(nanoTimeProvider.nowNanos() - preparationStartNanos);
            notifyTiming(
                    ProcessingMode.AST_TOKEN_STREAM,
                    preparationDurationMs,
                    0,
                    0,
                    totalStartNanos,
                    0,
                    0,
                    0,
                    false,
                    error);
            callback.onFailure(error);
            return;
        }
        long preparationDurationMs = toMillis(nanoTimeProvider.nowNanos() - preparationStartNanos);
        int totalTokenCount = totalTokenCount(preparationResult);
        int totalChunkCount = totalChunkCount(preparationResult);

        long translationStartNanos = nanoTimeProvider.nowNanos();
        structureTranslator.translate(
                preparationResult.getTokenizedDocument(),
                sourceLanguage,
                targetLanguage,
                timeoutMs,
                new MarkdownStructureTranslator.TokenizedTranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText, int chunkParseRecoveryCount) {
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
                                chunkParseRecoveryCount,
                                true,
                                null);
                        callback.onSuccess(translatedText);
                    }

                    @Override
                    public void onFailure(Exception error, int chunkParseRecoveryCount) {
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
                                chunkParseRecoveryCount,
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

        if (preparationResult.getTokenizedDocument() != null) {
            TokenizedMarkdownDocument tokenizedDocument = preparationResult.getTokenizedDocument();
            chunks = explainChunks(tokenizedDocument);
            tokens = explainTokens(tokenizedDocument);
        }

        return new ExplainMarkdownResult(
                preparationResult.getMode(),
                preparationResult.getMarkdownForTranslation(),
                tokens,
                chunks);
    }

    private void notifyTiming(
            ProcessingMode processingMode,
            long preparationDurationMs,
            long translationDurationMs,
            long restorationDurationMs,
            long totalStartNanos,
            int totalTokenCount,
            int totalChunkCount,
            int chunkParseRecoveryCount,
            boolean successful,
            Exception error) {
        if (translationMetricsListener == null) {
            return;
        }
        translationMetricsListener.onCompleted(
                new TranslationMetricsReport(
                        processingMode,
                        preparationDurationMs,
                        translationDurationMs,
                        restorationDurationMs,
                        toMillis(nanoTimeProvider.nowNanos() - totalStartNanos),
                        totalTokenCount,
                        totalChunkCount,
                        successful,
                        error,
                        chunkParseRecoveryCount));
    }

    private int totalChunkCount(MarkdownPreparationResult preparationResult) {
        if (preparationResult.getMode() == ProcessingMode.AST_TOKEN_STREAM
                && preparationResult.getTokenizedDocument() != null) {
            return structureTranslator
                    .chunkTranslatableTokens(preparationResult.getTokenizedDocument())
                    .size();
        }

        return 0;
    }

    private static int totalTokenCount(MarkdownPreparationResult preparationResult) {
        if (preparationResult.getTokenizedDocument() != null) {
            return preparationResult.getTokenizedDocument().getTokens().size();
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
