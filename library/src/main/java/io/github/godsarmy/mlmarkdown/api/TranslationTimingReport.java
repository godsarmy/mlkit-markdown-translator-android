package io.github.godsarmy.mlmarkdown.api;

import androidx.annotation.Nullable;
import io.github.godsarmy.mlmarkdown.markdown.ProcessingMode;

public final class TranslationTimingReport {
    private final ProcessingMode processingMode;
    private final long preparationDurationMs;
    private final long translationDurationMs;
    private final long restorationDurationMs;
    private final long totalDurationMs;
    private final int totalTokenCount;
    private final int totalChunkCount;
    private final boolean successful;
    @Nullable private final Exception error;
    private final int chunkParseRecoveryCount;
    private final boolean regexFallbackTriggered;

    public TranslationTimingReport(
            ProcessingMode processingMode,
            long preparationDurationMs,
            long translationDurationMs,
            long restorationDurationMs,
            long totalDurationMs,
            int totalTokenCount,
            int totalChunkCount,
            boolean successful,
            @Nullable Exception error,
            int chunkParseRecoveryCount,
            boolean regexFallbackTriggered) {
        this.processingMode = processingMode;
        this.preparationDurationMs = preparationDurationMs;
        this.translationDurationMs = translationDurationMs;
        this.restorationDurationMs = restorationDurationMs;
        this.totalDurationMs = totalDurationMs;
        this.totalTokenCount = totalTokenCount;
        this.totalChunkCount = totalChunkCount;
        this.successful = successful;
        this.error = error;
        this.chunkParseRecoveryCount = chunkParseRecoveryCount;
        this.regexFallbackTriggered = regexFallbackTriggered;
    }

    public ProcessingMode getProcessingMode() {
        return processingMode;
    }

    public long getPreparationDurationMs() {
        return preparationDurationMs;
    }

    public long getTranslationDurationMs() {
        return translationDurationMs;
    }

    public long getRestorationDurationMs() {
        return restorationDurationMs;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public int getTotalTokenCount() {
        return totalTokenCount;
    }

    public int getTotalChunkCount() {
        return totalChunkCount;
    }

    public boolean isSuccessful() {
        return successful;
    }

    @Nullable
    public Exception getError() {
        return error;
    }

    public int getChunkParseRecoveryCount() {
        return chunkParseRecoveryCount;
    }

    public boolean isRegexFallbackTriggered() {
        return regexFallbackTriggered;
    }
}
