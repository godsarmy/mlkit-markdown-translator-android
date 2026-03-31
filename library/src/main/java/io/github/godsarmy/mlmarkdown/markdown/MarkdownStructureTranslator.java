package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;
import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MarkdownStructureTranslator {
    private static final int DEFAULT_MAX_CHUNK_LENGTH =
            MarkdownTranslationOptions.DEFAULT_MAX_CHARS_PER_CHUNK;
    private static final String TOKEN_MARKER_PREFIX = "MLMD_TOKEN_";

    private final TranslationEngine translationEngine;
    private final int maxChunkLength;
    private final boolean preserveWhitespaceAroundProtectedSegments;
    private final String tokenMarker;

    public MarkdownStructureTranslator(TranslationEngine translationEngine) {
        this(
                translationEngine,
                DEFAULT_MAX_CHUNK_LENGTH,
                true,
                MarkdownTranslationOptions.DEFAULT_TOKEN_MARKER);
    }

    public MarkdownStructureTranslator(
            TranslationEngine translationEngine, MarkdownTranslationOptions options) {
        this(
                translationEngine,
                options.maxCharsPerChunk(),
                options.preserveWhitespaceAroundProtectedSegments(),
                options.tokenMarker());
    }

    MarkdownStructureTranslator(
            TranslationEngine translationEngine,
            boolean preserveWhitespaceAroundProtectedSegments) {
        this(
                translationEngine,
                preserveWhitespaceAroundProtectedSegments,
                MarkdownTranslationOptions.DEFAULT_TOKEN_MARKER);
    }

    MarkdownStructureTranslator(
            TranslationEngine translationEngine,
            boolean preserveWhitespaceAroundProtectedSegments,
            String tokenMarker) {
        this(
                translationEngine,
                DEFAULT_MAX_CHUNK_LENGTH,
                preserveWhitespaceAroundProtectedSegments,
                tokenMarker);
    }

    MarkdownStructureTranslator(TranslationEngine translationEngine, int maxChunkLength) {
        this(
                translationEngine,
                maxChunkLength,
                true,
                MarkdownTranslationOptions.DEFAULT_TOKEN_MARKER);
    }

    MarkdownStructureTranslator(
            TranslationEngine translationEngine,
            int maxChunkLength,
            boolean preserveWhitespaceAroundProtectedSegments) {
        this(
                translationEngine,
                maxChunkLength,
                preserveWhitespaceAroundProtectedSegments,
                MarkdownTranslationOptions.DEFAULT_TOKEN_MARKER);
    }

    MarkdownStructureTranslator(
            TranslationEngine translationEngine,
            int maxChunkLength,
            boolean preserveWhitespaceAroundProtectedSegments,
            String tokenMarker) {
        this.translationEngine = translationEngine;
        this.maxChunkLength = maxChunkLength;
        this.preserveWhitespaceAroundProtectedSegments = preserveWhitespaceAroundProtectedSegments;
        this.tokenMarker = tokenMarker;
    }

    public void translate(
            String markdown,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback) {
        translationEngine.translate(markdown, sourceLanguage, targetLanguage, callback);
    }

    public void translate(
            TokenizedMarkdownDocument tokenizedDocument,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback) {
        translate(
                tokenizedDocument,
                sourceLanguage,
                targetLanguage,
                new TokenizedTranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText, int chunkParseRecoveryCount) {
                        callback.onSuccess(translatedText);
                    }

                    @Override
                    public void onFailure(Exception error, int chunkParseRecoveryCount) {
                        callback.onFailure(error);
                    }
                });
    }

    void translate(
            TokenizedMarkdownDocument tokenizedDocument,
            String sourceLanguage,
            String targetLanguage,
            TokenizedTranslationCallback callback) {
        List<TranslationChunk> chunks = chunkTranslatableTokens(tokenizedDocument);
        if (chunks.isEmpty()) {
            callback.onSuccess(tokenizedDocument.reconstruct(), 0);
            return;
        }

        translateChunks(
                tokenizedDocument,
                chunks.iterator(),
                new LinkedHashMap<>(),
                sourceLanguage,
                targetLanguage,
                callback,
                0);
    }

    List<TranslationChunk> chunkTranslatableTokens(TokenizedMarkdownDocument tokenizedDocument) {
        List<TranslationChunk> chunks = new ArrayList<>();
        TranslationChunkBuilder currentChunk = new TranslationChunkBuilder();

        for (MarkdownToken token : tokenizedDocument.getTokens()) {
            if (token.getType() == MarkdownTokenType.TRANSLATABLE && token.getTokenId() != null) {
                if (!currentChunk.isEmpty()
                        && currentChunk.wouldExceedLimit(token, maxChunkLength)) {
                    chunks.add(currentChunk.build());
                    currentChunk = new TranslationChunkBuilder();
                }
                currentChunk.append(token);
                continue;
            }

            if (containsLineBoundary(token) && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.build());
                currentChunk = new TranslationChunkBuilder();
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.build());
        }

        return chunks;
    }

    private void translateChunks(
            TokenizedMarkdownDocument tokenizedDocument,
            Iterator<TranslationChunk> iterator,
            Map<String, String> translations,
            String sourceLanguage,
            String targetLanguage,
            TokenizedTranslationCallback callback,
            int chunkParseRecoveryCount) {
        if (!iterator.hasNext()) {
            callback.onSuccess(
                    tokenizedDocument.reconstructWithTranslations(translations),
                    chunkParseRecoveryCount);
            return;
        }

        TranslationChunk chunk = iterator.next();
        translationEngine.translate(
                chunk.getText(),
                sourceLanguage,
                targetLanguage,
                new TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        try {
                            translations.putAll(parseChunkTranslations(chunk, translatedText));
                            translateChunks(
                                    tokenizedDocument,
                                    iterator,
                                    translations,
                                    sourceLanguage,
                                    targetLanguage,
                                    callback,
                                    chunkParseRecoveryCount);
                        } catch (IllegalStateException parseError) {
                            translateChunkIndividually(
                                    tokenizedDocument,
                                    chunk,
                                    iterator,
                                    translations,
                                    sourceLanguage,
                                    targetLanguage,
                                    callback,
                                    chunkParseRecoveryCount + 1);
                        }
                    }

                    @Override
                    public void onFailure(Exception error) {
                        callback.onFailure(error, chunkParseRecoveryCount);
                    }
                });
    }

    private void translateChunkIndividually(
            TokenizedMarkdownDocument tokenizedDocument,
            TranslationChunk chunk,
            Iterator<TranslationChunk> iterator,
            Map<String, String> translations,
            String sourceLanguage,
            String targetLanguage,
            TokenizedTranslationCallback callback,
            int chunkParseRecoveryCount) {
        translateChunkTokenAt(
                tokenizedDocument,
                chunk,
                iterator,
                translations,
                sourceLanguage,
                targetLanguage,
                callback,
                chunkParseRecoveryCount,
                0);
    }

    private void translateChunkTokenAt(
            TokenizedMarkdownDocument tokenizedDocument,
            TranslationChunk chunk,
            Iterator<TranslationChunk> iterator,
            Map<String, String> translations,
            String sourceLanguage,
            String targetLanguage,
            TokenizedTranslationCallback callback,
            int chunkParseRecoveryCount,
            int index) {
        if (index >= chunk.getTokenIds().size()) {
            translateChunks(
                    tokenizedDocument,
                    iterator,
                    translations,
                    sourceLanguage,
                    targetLanguage,
                    callback,
                    chunkParseRecoveryCount);
            return;
        }

        String tokenId = chunk.getTokenIds().get(index);
        String tokenValue = chunk.getTokenValues().get(index);
        translationEngine.translate(
                tokenValue,
                sourceLanguage,
                targetLanguage,
                new TranslationCallback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        translations.put(
                                tokenId, maybePreserveEdgeWhitespace(tokenValue, translatedText));
                        translateChunkTokenAt(
                                tokenizedDocument,
                                chunk,
                                iterator,
                                translations,
                                sourceLanguage,
                                targetLanguage,
                                callback,
                                chunkParseRecoveryCount,
                                index + 1);
                    }

                    @Override
                    public void onFailure(Exception error) {
                        callback.onFailure(error, chunkParseRecoveryCount);
                    }
                });
    }

    interface TokenizedTranslationCallback {
        void onSuccess(String translatedText, int chunkParseRecoveryCount);

        void onFailure(Exception error, int chunkParseRecoveryCount);
    }

    private static boolean containsLineBoundary(MarkdownToken token) {
        return token.getValue().contains("\n");
    }

    private Map<String, String> parseChunkTranslations(
            TranslationChunk chunk, String translatedText) {
        Map<String, String> translations = new LinkedHashMap<>();
        int searchFrom = 0;

        for (int i = 0; i < chunk.getTokenIds().size(); i++) {
            String tokenId = chunk.getTokenIds().get(i);
            String sourceTokenValue = chunk.getTokenValues().get(i);
            String marker = markerFor(tokenId);
            int markerStart = translatedText.indexOf(marker, searchFrom);
            if (markerStart < 0) {
                throw new IllegalStateException(
                        "Translated chunk is missing marker for token " + tokenId);
            }

            int valueStart = markerStart + marker.length();
            int valueEnd = translatedText.length();
            if (i + 1 < chunk.getTokenIds().size()) {
                String nextMarker = markerFor(chunk.getTokenIds().get(i + 1));
                valueEnd = translatedText.indexOf(nextMarker, valueStart);
                if (valueEnd < 0) {
                    throw new IllegalStateException(
                            "Translated chunk is missing next marker after token " + tokenId);
                }
            }

            String translatedValue = translatedText.substring(valueStart, valueEnd);
            translations.put(
                    tokenId, maybePreserveEdgeWhitespace(sourceTokenValue, translatedValue));
            searchFrom = valueEnd;
        }

        return translations;
    }

    private String markerFor(String tokenId) {
        return tokenMarker + TOKEN_MARKER_PREFIX + tokenId + tokenMarker;
    }

    private String maybePreserveEdgeWhitespace(String sourceValue, String translatedValue) {
        if (!preserveWhitespaceAroundProtectedSegments) {
            return translatedValue;
        }
        return preserveEdgeWhitespace(sourceValue, translatedValue);
    }

    private static String preserveEdgeWhitespace(String sourceValue, String translatedValue) {
        if (translatedValue == null || translatedValue.isEmpty()) {
            return translatedValue;
        }

        String leadingWhitespace = leadingWhitespace(sourceValue);
        String trailingWhitespace = trailingWhitespace(sourceValue);
        String result = translatedValue;

        if (!leadingWhitespace.isEmpty() && !Character.isWhitespace(result.charAt(0))) {
            result = leadingWhitespace + result;
        }

        if (!trailingWhitespace.isEmpty()
                && !Character.isWhitespace(result.charAt(result.length() - 1))) {
            result = result + trailingWhitespace;
        }

        return result;
    }

    private static String leadingWhitespace(String value) {
        int index = 0;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return value.substring(0, index);
    }

    private static String trailingWhitespace(String value) {
        int index = value.length() - 1;
        while (index >= 0 && Character.isWhitespace(value.charAt(index))) {
            index--;
        }
        return value.substring(index + 1);
    }

    static final class TranslationChunk {
        private final List<String> tokenIds;
        private final List<String> tokenValues;
        private final String text;

        private TranslationChunk(List<String> tokenIds, List<String> tokenValues, String text) {
            this.tokenIds = List.copyOf(tokenIds);
            this.tokenValues = List.copyOf(tokenValues);
            this.text = text;
        }

        List<String> getTokenIds() {
            return tokenIds;
        }

        String getText() {
            return text;
        }

        List<String> getTokenValues() {
            return tokenValues;
        }
    }

    private final class TranslationChunkBuilder {
        private final List<String> tokenIds = new ArrayList<>();
        private final List<String> tokenValues = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();
        private int plainTextLength;

        private boolean isEmpty() {
            return tokenIds.isEmpty();
        }

        private boolean wouldExceedLimit(MarkdownToken token, int maxChunkLength) {
            return !isEmpty() && plainTextLength + token.getValue().length() > maxChunkLength;
        }

        private void append(MarkdownToken token) {
            tokenIds.add(token.getTokenId());
            tokenValues.add(token.getValue());
            text.append(markerFor(token.getTokenId()));
            text.append(token.getValue());
            plainTextLength += token.getValue().length();
        }

        private TranslationChunk build() {
            return new TranslationChunk(tokenIds, tokenValues, text.toString());
        }
    }
}
