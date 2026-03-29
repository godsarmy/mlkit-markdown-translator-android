package io.github.godsarmy.mlmarkdown.api;

import io.github.godsarmy.mlmarkdown.markdown.ProcessingMode;
import java.util.List;

public final class ExplainMarkdownResult {
    private final ProcessingMode processingMode;
    private final String preparedMarkdown;
    private final List<ExplainMarkdownToken> tokens;
    private final List<ExplainMarkdownChunk> chunks;
    private final List<ExplainProtectedSegment> protectedSegments;

    public ExplainMarkdownResult(
            ProcessingMode processingMode,
            String preparedMarkdown,
            List<ExplainMarkdownToken> tokens,
            List<ExplainMarkdownChunk> chunks,
            List<ExplainProtectedSegment> protectedSegments) {
        this.processingMode = processingMode;
        this.preparedMarkdown = preparedMarkdown;
        this.tokens = List.copyOf(tokens);
        this.chunks = List.copyOf(chunks);
        this.protectedSegments = List.copyOf(protectedSegments);
    }

    public ProcessingMode getProcessingMode() {
        return processingMode;
    }

    public String getPreparedMarkdown() {
        return preparedMarkdown;
    }

    public List<ExplainMarkdownToken> getTokens() {
        return tokens;
    }

    public List<ExplainMarkdownChunk> getChunks() {
        return chunks;
    }

    public List<ExplainProtectedSegment> getProtectedSegments() {
        return protectedSegments;
    }

    public int getTotalTokenCount() {
        return tokens.size();
    }

    public int getTotalChunkCount() {
        return chunks.size();
    }
}
