package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;
import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MarkdownStructureTranslator {
    private static final int DEFAULT_MAX_CHUNK_LENGTH = 400;
    private static final String TOKEN_MARKER_PREFIX = "@@MLMD_TOKEN_";
    private static final String TOKEN_MARKER_SUFFIX = "@@";

    private final TranslationEngine translationEngine;
    private final int maxChunkLength;

    public MarkdownStructureTranslator(TranslationEngine translationEngine) {
        this(translationEngine, DEFAULT_MAX_CHUNK_LENGTH);
    }

    MarkdownStructureTranslator(TranslationEngine translationEngine, int maxChunkLength) {
        this.translationEngine = translationEngine;
        this.maxChunkLength = maxChunkLength;
    }

    public void translate(String markdown, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
        translationEngine.translate(markdown, sourceLanguage, targetLanguage, callback);
    }

    public void translate(
            TokenizedMarkdownDocument tokenizedDocument,
            String sourceLanguage,
            String targetLanguage,
            TranslationCallback callback
    ) {
        List<TranslationChunk> chunks = chunkTranslatableTokens(tokenizedDocument);
        if (chunks.isEmpty()) {
            callback.onSuccess(tokenizedDocument.reconstruct());
            return;
        }

        translateChunks(
                tokenizedDocument,
                chunks.iterator(),
                new LinkedHashMap<>(),
                sourceLanguage,
                targetLanguage,
                callback
        );
    }

    List<TranslationChunk> chunkTranslatableTokens(TokenizedMarkdownDocument tokenizedDocument) {
        List<TranslationChunk> chunks = new ArrayList<>();
        TranslationChunkBuilder currentChunk = new TranslationChunkBuilder();

        for (MarkdownToken token : tokenizedDocument.getTokens()) {
            if (token.getType() == MarkdownTokenType.TRANSLATABLE && token.getTokenId() != null) {
                if (!currentChunk.isEmpty() && currentChunk.wouldExceedLimit(token, maxChunkLength)) {
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
            TranslationCallback callback
    ) {
        if (!iterator.hasNext()) {
            callback.onSuccess(tokenizedDocument.reconstructWithTranslations(translations));
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
                                    callback
                            );
                        } catch (IllegalStateException parseError) {
                            callback.onFailure(parseError);
                        }
                    }

                    @Override
                    public void onFailure(Exception error) {
                        callback.onFailure(error);
                    }
                }
        );
    }

    private static boolean containsLineBoundary(MarkdownToken token) {
        return token.getValue().contains("\n");
    }

    private static Map<String, String> parseChunkTranslations(TranslationChunk chunk, String translatedText) {
        Map<String, String> translations = new LinkedHashMap<>();
        int searchFrom = 0;

        for (int i = 0; i < chunk.getTokenIds().size(); i++) {
            String tokenId = chunk.getTokenIds().get(i);
            String marker = markerFor(tokenId);
            int markerStart = translatedText.indexOf(marker, searchFrom);
            if (markerStart < 0) {
                throw new IllegalStateException("Translated chunk is missing marker for token " + tokenId);
            }

            int valueStart = markerStart + marker.length();
            int valueEnd = translatedText.length();
            if (i + 1 < chunk.getTokenIds().size()) {
                String nextMarker = markerFor(chunk.getTokenIds().get(i + 1));
                valueEnd = translatedText.indexOf(nextMarker, valueStart);
                if (valueEnd < 0) {
                    throw new IllegalStateException(
                            "Translated chunk is missing next marker after token " + tokenId
                    );
                }
            }

            translations.put(tokenId, translatedText.substring(valueStart, valueEnd));
            searchFrom = valueEnd;
        }

        return translations;
    }

    private static String markerFor(String tokenId) {
        return TOKEN_MARKER_PREFIX + tokenId + TOKEN_MARKER_SUFFIX;
    }

    static final class TranslationChunk {
        private final List<String> tokenIds;
        private final String text;

        private TranslationChunk(List<String> tokenIds, String text) {
            this.tokenIds = List.copyOf(tokenIds);
            this.text = text;
        }

        List<String> getTokenIds() {
            return tokenIds;
        }

        String getText() {
            return text;
        }
    }

    private static final class TranslationChunkBuilder {
        private final List<String> tokenIds = new ArrayList<>();
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
            text.append(markerFor(token.getTokenId()));
            text.append(token.getValue());
            plainTextLength += token.getValue().length();
        }

        private TranslationChunk build() {
            return new TranslationChunk(tokenIds, text.toString());
        }
    }
}
