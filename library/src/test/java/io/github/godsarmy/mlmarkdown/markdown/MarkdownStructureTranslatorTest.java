package io.github.godsarmy.mlmarkdown.markdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;
import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class MarkdownStructureTranslatorTest {
    @Test
    public void chunkTranslatableTokens_splitsOnLineBoundaryTokens() {
        MarkdownStructureTranslator translator =
                new MarkdownStructureTranslator(new EchoTranslationEngine(), 200);
        TokenizedMarkdownDocument document =
                new TokenizedMarkdownDocument(
                        "first\n\nsecond",
                        List.of(
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T1", "first", 0, 5),
                                new MarkdownToken(MarkdownTokenType.STRUCTURAL, "\n\n", 5, 7),
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T2", "second", 7, 13)));

        List<MarkdownStructureTranslator.TranslationChunk> chunks =
                translator.chunkTranslatableTokens(document);

        assertEquals(2, chunks.size());
        assertEquals(List.of("T1"), chunks.get(0).getTokenIds());
        assertEquals(List.of("T2"), chunks.get(1).getTokenIds());
    }

    @Test
    public void chunkTranslatableTokens_splitsWhenChunkExceedsMaxLength() {
        MarkdownStructureTranslator translator =
                new MarkdownStructureTranslator(new EchoTranslationEngine(), 5);
        TokenizedMarkdownDocument document =
                new TokenizedMarkdownDocument(
                        "alphabeta",
                        List.of(
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T1", "alpha", 0, 5),
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T2", "beta", 5, 9)));

        List<MarkdownStructureTranslator.TranslationChunk> chunks =
                translator.chunkTranslatableTokens(document);

        assertEquals(2, chunks.size());
        assertEquals(List.of("T1"), chunks.get(0).getTokenIds());
        assertEquals(List.of("T2"), chunks.get(1).getTokenIds());
    }

    @Test
    public void chunkTranslatableTokens_usesConfiguredTokenMarker() {
        MarkdownStructureTranslator translator =
                new MarkdownStructureTranslator(new EchoTranslationEngine(), 200, true, "##");
        TokenizedMarkdownDocument document =
                new TokenizedMarkdownDocument(
                        "first",
                        List.of(
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T1", "first", 0, 5)));

        List<MarkdownStructureTranslator.TranslationChunk> chunks =
                translator.chunkTranslatableTokens(document);

        assertEquals(1, chunks.size());
        assertEquals("##MLMD_TOKEN_T1##first", chunks.get(0).getText());
    }

    @Test
    public void translate_returnsOriginalWhenNoTranslatableTokens() {
        RecordingTranslationEngine engine = new RecordingTranslationEngine();
        MarkdownStructureTranslator translator = new MarkdownStructureTranslator(engine, 100);
        TokenizedMarkdownDocument document =
                new TokenizedMarkdownDocument(
                        "```code```",
                        List.of(
                                new MarkdownToken(
                                        MarkdownTokenType.PROTECTED, "```code```", 0, 10)));
        TestCallback callback = new TestCallback();

        translator.translate(document, "en", "es", callback);

        assertEquals("```code```", callback.translatedText);
        assertNull(callback.error);
        assertTrue(engine.inputs.isEmpty());
    }

    @Test
    public void translate_propagatesFailureDuringPerTokenFallback() {
        FallbackFailureEngine engine = new FallbackFailureEngine();
        MarkdownStructureTranslator translator = new MarkdownStructureTranslator(engine, 200);
        TokenizedMarkdownDocument document =
                new TokenizedMarkdownDocument(
                        "first second",
                        List.of(
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T1", "first", 0, 5),
                                new MarkdownToken(MarkdownTokenType.STRUCTURAL, " ", 5, 6),
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T2", "second", 6, 12)));
        TestCallback callback = new TestCallback();

        translator.translate(document, "en", "es", callback);

        assertNotNull(callback.error);
        assertEquals("token fail", callback.error.getMessage());
        assertNull(callback.translatedText);
    }

    @Test
    public void
            translate_preservesSpacesAroundProtectedSegments_whenChunkTranslationTrimsWhitespace() {
        MarkdownStructureTranslator translator =
                new MarkdownStructureTranslator(new ChunkTrimmingEngine(), 200);
        TokenizedMarkdownDocument document =
                new TokenizedMarkdownDocument(
                        "Open <https://example.com> now",
                        List.of(
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T1", "Open ", 0, 5),
                                new MarkdownToken(
                                        MarkdownTokenType.PROTECTED,
                                        "<https://example.com>",
                                        5,
                                        26),
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T2", " now", 26, 30)));
        TestCallback callback = new TestCallback();

        translator.translate(document, "en", "es", callback);

        assertEquals("TR(Open) <https://example.com> TR(now)", callback.translatedText);
        assertNull(callback.error);
    }

    @Test
    public void translate_preservesSpacesAroundProtectedSegments_inPerTokenFallbackPath() {
        MarkdownStructureTranslator translator =
                new MarkdownStructureTranslator(new MarkerStrippingTrimEngine(), 200);
        TokenizedMarkdownDocument document =
                new TokenizedMarkdownDocument(
                        "Run `echo hello` now",
                        List.of(
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T1", "Run ", 0, 4),
                                new MarkdownToken(
                                        MarkdownTokenType.PROTECTED, "`echo hello`", 4, 16),
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T2", " now", 16, 20)));
        TestCallback callback = new TestCallback();

        translator.translate(document, "en", "es", callback);

        assertEquals("TR(Run) `echo hello` TR(now)", callback.translatedText);
        assertNull(callback.error);
    }

    @Test
    public void translate_canDisableWhitespacePreservationAroundProtectedSegments() {
        MarkdownStructureTranslator translator =
                new MarkdownStructureTranslator(new ChunkTrimmingEngine(), 200, false);
        TokenizedMarkdownDocument document =
                new TokenizedMarkdownDocument(
                        "Open <https://example.com> now",
                        List.of(
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T1", "Open ", 0, 5),
                                new MarkdownToken(
                                        MarkdownTokenType.PROTECTED,
                                        "<https://example.com>",
                                        5,
                                        26),
                                new MarkdownToken(
                                        MarkdownTokenType.TRANSLATABLE, "T2", " now", 26, 30)));
        TestCallback callback = new TestCallback();

        translator.translate(document, "en", "es", callback);

        assertEquals("TR(Open)<https://example.com>TR(now)", callback.translatedText);
        assertNull(callback.error);
    }

    private static final class EchoTranslationEngine implements TranslationEngine {
        @Override
        public void translate(
                String text,
                String sourceLanguage,
                String targetLanguage,
                TranslationCallback callback) {
            callback.onSuccess(text);
        }
    }

    private static final class RecordingTranslationEngine implements TranslationEngine {
        private final List<String> inputs = new ArrayList<>();

        @Override
        public void translate(
                String text,
                String sourceLanguage,
                String targetLanguage,
                TranslationCallback callback) {
            inputs.add(text);
            callback.onSuccess(text);
        }
    }

    private static final class FallbackFailureEngine implements TranslationEngine {
        private boolean chunkAttempted;

        @Override
        public void translate(
                String text,
                String sourceLanguage,
                String targetLanguage,
                TranslationCallback callback) {
            if (text.contains("@@MLMD_TOKEN_") && !chunkAttempted) {
                chunkAttempted = true;
                callback.onSuccess(text.replaceAll("@@MLMD_TOKEN_[^@]+@@", ""));
                return;
            }

            if ("second".equals(text)) {
                callback.onFailure(new IllegalStateException("token fail"));
                return;
            }

            callback.onSuccess("TR(" + text + ")");
        }
    }

    private static final class ChunkTrimmingEngine implements TranslationEngine {
        private static final Pattern MARKER_PATTERN = Pattern.compile("@@MLMD_TOKEN_[^@]+@@");

        @Override
        public void translate(
                String text,
                String sourceLanguage,
                String targetLanguage,
                TranslationCallback callback) {
            if (!text.contains("@@MLMD_TOKEN_")) {
                callback.onSuccess("TR(" + text.trim() + ")");
                return;
            }

            Matcher matcher = MARKER_PATTERN.matcher(text);
            StringBuilder translated = new StringBuilder();

            while (matcher.find()) {
                translated.append(matcher.group());
                String segment =
                        text.substring(matcher.end(), nextMarkerStart(text, matcher.end()));
                translated.append("TR(").append(segment.trim()).append(")");
            }

            callback.onSuccess(translated.toString());
        }

        private static int nextMarkerStart(String text, int from) {
            Matcher matcher = MARKER_PATTERN.matcher(text);
            return matcher.find(from) ? matcher.start() : text.length();
        }
    }

    private static final class MarkerStrippingTrimEngine implements TranslationEngine {
        private static final Pattern MARKER_PATTERN = Pattern.compile("@@MLMD_TOKEN_[^@]+@@");

        @Override
        public void translate(
                String text,
                String sourceLanguage,
                String targetLanguage,
                TranslationCallback callback) {
            if (text.contains("@@MLMD_TOKEN_")) {
                callback.onSuccess(MARKER_PATTERN.matcher(text).replaceAll(""));
                return;
            }

            callback.onSuccess("TR(" + text.trim() + ")");
        }
    }

    private static final class TestCallback implements TranslationCallback {
        private String translatedText;
        private Exception error;

        @Override
        public void onSuccess(String translatedText) {
            this.translatedText = translatedText;
        }

        @Override
        public void onFailure(Exception error) {
            this.error = error;
        }
    }
}
