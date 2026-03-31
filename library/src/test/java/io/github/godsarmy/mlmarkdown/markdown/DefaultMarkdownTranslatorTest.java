package io.github.godsarmy.mlmarkdown.markdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownResult;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.api.TranslationTimingListener;
import io.github.godsarmy.mlmarkdown.api.TranslationTimingReport;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class DefaultMarkdownTranslatorTest {
    @Test
    public void translateMarkdown_preservesStructureWhileTranslatingAstTextTokens() {
        RecordingTranslationEngine engine = new RecordingTranslationEngine();
        DefaultMarkdownTranslator translator = new DefaultMarkdownTranslator(engine);

        String source =
                "# Title\n\n"
                        + "- install package\n"
                        + "> quote text\n\n"
                        + "Paragraph with `code` and [label](https://example.com) and ![alt](https://example.com/image.png).\n\n"
                        + "```bash\n"
                        + "echo hello\n"
                        + "```\n";

        TestTranslationCallback callback = new TestTranslationCallback();
        translator.translateMarkdown(source, "en", "es", callback);

        assertEquals(
                "# TR(Title)\n\n"
                        + "- TR(install package)\n"
                        + "> TR(quote text)\n\n"
                        + "TR(Paragraph with ) `code` TR( and ) [TR(label)](https://example.com) TR( and ) ![TR(alt)](https://example.com/image.png)TR(.)\n\n"
                        + "```bash\n"
                        + "echo hello\n"
                        + "```\n",
                callback.translatedText);

        assertEquals(
                List.of(
                        "Title",
                        "install package",
                        "quote text",
                        "Paragraph with | and |label| and |alt|."),
                engine.inputs);
    }

    @Test
    public void translateMarkdown_splitsLargeParagraphsIntoMultipleChunks() {
        MarkdownStructureTranslator structureTranslator =
                new MarkdownStructureTranslator(new RecordingTranslationEngine(), 20);
        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        TestTranslationCallback callback = new TestTranslationCallback();

        structureTranslator.translate(
                builder.build("alpha *beta* gamma **delta** epsilon zeta"), "en", "es", callback);

        assertEquals(
                "TR(alpha ) *TR(beta)* TR( gamma ) **TR(delta)** TR( epsilon zeta)",
                callback.translatedText);
    }

    @Test
    public void translateMarkdown_propagatesTranslationEngineFailure() {
        DefaultMarkdownTranslator translator =
                new DefaultMarkdownTranslator(new FailingTranslationEngine());

        TestTranslationCallback callback = new TestTranslationCallback();
        translator.translateMarkdown("# Title", "en", "es", callback);

        assertTrue(callback.error instanceof IllegalStateException);
        assertEquals("boom", callback.error.getMessage());
    }

    @Test
    public void translateMarkdown_fallsBackWhenMarkersAreMissingFromChunkResponse() {
        DefaultMarkdownTranslator translator =
                new DefaultMarkdownTranslator(new MarkerStrippingTranslationEngine());

        String source =
                "# Title\n\n"
                        + "- install package\n"
                        + "> quote text\n\n"
                        + "Paragraph with [label](https://example.com).\n";

        TestTranslationCallback callback = new TestTranslationCallback();
        translator.translateMarkdown(source, "en", "es", callback);

        assertEquals(
                "# TR(Title)\n\n"
                        + "- TR(install package)\n"
                        + "> TR(quote text)\n\n"
                        + "TR(Paragraph with ) [TR(label)](https://example.com)TR(.)\n",
                callback.translatedText);
        assertEquals(null, callback.error);
    }

    @Test
    public void translateMarkdown_canDisableWhitespacePreservationAroundProtectedSegments() {
        DefaultMarkdownTranslator translator =
                new DefaultMarkdownTranslator(
                        new MarkerStrippingTrimTranslationEngine(),
                        new MarkdownTranslationOptions.Builder()
                                .setPreserveWhitespaceAroundProtectedSegments(false)
                                .build());

        String source = "Run `echo hello` now";
        TestTranslationCallback callback = new TestTranslationCallback();

        translator.translateMarkdown(source, "en", "ja", callback);

        assertEquals("TR(Run)`echo hello`TR(now)", callback.translatedText);
        assertEquals(null, callback.error);
    }

    @Test
    public void translateMarkdown_reportsStageTimingsOnSuccess() {
        RecordingTimingListener timingListener = new RecordingTimingListener();
        FakeNanoTimeProvider nanoTimeProvider =
                new FakeNanoTimeProvider(
                        0L, 1_000_000L, 4_000_000L, 7_000_000L, 9_000_000L, 12_000_000L);
        DefaultMarkdownTranslator translator =
                new DefaultMarkdownTranslator(
                        new MarkerStrippingTranslationEngine(),
                        new MarkdownTranslationOptions.Builder()
                                .setTranslationTimingListener(timingListener)
                                .build(),
                        nanoTimeProvider);

        TestTranslationCallback callback = new TestTranslationCallback();
        translator.translateMarkdown("Run `echo hello` now", "en", "es", callback);

        assertNotNull(callback.translatedText);
        assertNull(callback.error);
        assertNotNull(timingListener.lastReport);
        assertTrue(timingListener.lastReport.isSuccessful());
        assertEquals(
                ProcessingMode.AST_TOKEN_STREAM, timingListener.lastReport.getProcessingMode());
        assertEquals(3L, timingListener.lastReport.getPreparationDurationMs());
        assertEquals(2L, timingListener.lastReport.getTranslationDurationMs());
        assertEquals(0L, timingListener.lastReport.getRestorationDurationMs());
        assertEquals(12L, timingListener.lastReport.getTotalDurationMs());
        assertTrue(timingListener.lastReport.getTotalTokenCount() > 0);
        assertEquals(1, timingListener.lastReport.getTotalChunkCount());
        assertEquals(1, timingListener.lastReport.getChunkParseRecoveryCount());
        assertNull(timingListener.lastReport.getError());
    }

    @Test
    public void translateMarkdown_reportsStageTimingsOnFailure() {
        RecordingTimingListener timingListener = new RecordingTimingListener();
        FakeNanoTimeProvider nanoTimeProvider =
                new FakeNanoTimeProvider(0L, 2_000_000L, 5_000_000L, 8_000_000L, 11_000_000L);
        DefaultMarkdownTranslator translator =
                new DefaultMarkdownTranslator(
                        new FailingTranslationEngine(),
                        new MarkdownTranslationOptions.Builder()
                                .setTranslationTimingListener(timingListener)
                                .build(),
                        nanoTimeProvider);

        TestTranslationCallback callback = new TestTranslationCallback();
        translator.translateMarkdown("# Title", "en", "es", callback);

        assertNull(callback.translatedText);
        assertNotNull(callback.error);
        assertNotNull(timingListener.lastReport);
        assertFalse(timingListener.lastReport.isSuccessful());
        assertEquals(
                ProcessingMode.AST_TOKEN_STREAM, timingListener.lastReport.getProcessingMode());
        assertEquals(3L, timingListener.lastReport.getPreparationDurationMs());
        assertEquals(3L, timingListener.lastReport.getTranslationDurationMs());
        assertEquals(0L, timingListener.lastReport.getRestorationDurationMs());
        assertEquals(11L, timingListener.lastReport.getTotalDurationMs());
        assertTrue(timingListener.lastReport.getTotalTokenCount() > 0);
        assertEquals(1, timingListener.lastReport.getTotalChunkCount());
        assertEquals(0, timingListener.lastReport.getChunkParseRecoveryCount());
        assertNotNull(timingListener.lastReport.getError());
        assertEquals("boom", timingListener.lastReport.getError().getMessage());
    }

    @Test
    public void explainMarkdown_returnsChunkDiagnosticsWithoutCallingTranslationEngine() {
        RecordingTranslationEngine engine = new RecordingTranslationEngine();
        DefaultMarkdownTranslator translator = new DefaultMarkdownTranslator(engine);

        ExplainMarkdownResult result =
                translator.explainMarkdown(
                        "# Title\n\nParagraph with [label](https://example.com) and `code`.\n");

        assertEquals(ProcessingMode.AST_TOKEN_STREAM, result.getProcessingMode());
        assertNotEquals(0, result.getTotalTokenCount());
        assertNotEquals(0, result.getTotalChunkCount());
        assertTrue(result.getChunks().get(0).getRawText().contains("@@MLMD_TOKEN_"));
        assertEquals(0, engine.inputs.size());
    }

    @Test
    public void explainMarkdown_includesTokenMetadataFromAstModel() {
        DefaultMarkdownTranslator translator =
                new DefaultMarkdownTranslator(new RecordingTranslationEngine());

        ExplainMarkdownResult result = translator.explainMarkdown("# Hello");

        assertEquals(ProcessingMode.AST_TOKEN_STREAM, result.getProcessingMode());
        assertNotEquals(0, result.getTokens().size());
        assertEquals(0, result.getProtectedSegments().size());
        assertEquals("# Hello", result.getPreparedMarkdown());
    }

    private static class RecordingTranslationEngine implements TranslationEngine {
        private static final Pattern MARKER_PATTERN = Pattern.compile("@@MLMD_TOKEN_[^@]+@@");
        private final java.util.ArrayList<String> inputs = new java.util.ArrayList<>();

        @Override
        public void translate(
                String text,
                String sourceLanguage,
                String targetLanguage,
                TranslationCallback callback) {
            Matcher matcher = MARKER_PATTERN.matcher(text);
            StringBuilder normalizedInput = new StringBuilder();
            StringBuilder translated = new StringBuilder();
            int lastEnd = 0;

            while (matcher.find()) {
                String segment = text.substring(lastEnd, matcher.start());
                if (!segment.isEmpty()) {
                    if (normalizedInput.length() > 0) {
                        normalizedInput.append('|');
                    }
                    normalizedInput.append(segment);
                    translated.append("TR(").append(segment).append(")");
                }
                translated.append(matcher.group());
                lastEnd = matcher.end();
            }

            String trailingSegment = text.substring(lastEnd);
            if (!trailingSegment.isEmpty()) {
                if (normalizedInput.length() > 0) {
                    normalizedInput.append('|');
                }
                normalizedInput.append(trailingSegment);
                translated.append("TR(").append(trailingSegment).append(")");
            }

            inputs.add(normalizedInput.toString());
            callback.onSuccess(translated.toString());
        }
    }

    private static final class FailingTranslationEngine implements TranslationEngine {
        @Override
        public void translate(
                String text,
                String sourceLanguage,
                String targetLanguage,
                TranslationCallback callback) {
            callback.onFailure(new IllegalStateException("boom"));
        }
    }

    private static final class MarkerStrippingTranslationEngine implements TranslationEngine {
        private static final Pattern MARKER_PATTERN = Pattern.compile("@@MLMD_TOKEN_[^@]+@@");

        @Override
        public void translate(
                String text,
                String sourceLanguage,
                String targetLanguage,
                TranslationCallback callback) {
            if (text.contains("@@MLMD_TOKEN_")) {
                // Simulates real translator behavior that mutates or removes markers.
                String withoutMarkers = MARKER_PATTERN.matcher(text).replaceAll("");
                callback.onSuccess("TR(" + withoutMarkers + ")");
                return;
            }
            callback.onSuccess("TR(" + text + ")");
        }
    }

    private static final class MarkerStrippingTrimTranslationEngine implements TranslationEngine {
        private static final Pattern MARKER_PATTERN = Pattern.compile("@@MLMD_TOKEN_[^@]+@@");

        @Override
        public void translate(
                String text,
                String sourceLanguage,
                String targetLanguage,
                TranslationCallback callback) {
            if (text.contains("@@MLMD_TOKEN_")) {
                String withoutMarkers = MARKER_PATTERN.matcher(text).replaceAll("");
                callback.onSuccess(withoutMarkers);
                return;
            }
            callback.onSuccess("TR(" + text.trim() + ")");
        }
    }

    private static final class TestTranslationCallback implements TranslationCallback {
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

    private static final class RecordingTimingListener implements TranslationTimingListener {
        private TranslationTimingReport lastReport;

        @Override
        public void onCompleted(TranslationTimingReport report) {
            this.lastReport = report;
        }
    }

    private static final class FakeNanoTimeProvider
            implements DefaultMarkdownTranslator.NanoTimeProvider {
        private final long[] values;
        private int index;

        private FakeNanoTimeProvider(long... values) {
            this.values = values;
        }

        @Override
        public long nowNanos() {
            if (index >= values.length) {
                return values[values.length - 1];
            }
            return values[index++];
        }
    }
}
