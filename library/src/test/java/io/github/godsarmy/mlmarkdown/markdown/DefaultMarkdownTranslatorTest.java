package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;

import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultMarkdownTranslatorTest {
    @Test
    public void translateMarkdown_preservesStructureWhileTranslatingAstTextTokens() {
        RecordingTranslationEngine engine = new RecordingTranslationEngine();
        DefaultMarkdownTranslator translator = new DefaultMarkdownTranslator(engine);

        String source = "# Title\n\n"
                + "- install package\n"
                + "> quote text\n\n"
                + "Paragraph with `code` and [label](https://example.com) and ![alt](https://example.com/image.png).\n\n"
                + "```bash\n"
                + "echo hello\n"
                + "```\n";

        TestTranslationCallback callback = new TestTranslationCallback();
        translator.translateMarkdown(source, "en", "es", callback);

        assertEquals("# TR(Title)\n\n"
                + "- TR(install package)\n"
                + "> TR(quote text)\n\n"
                + "TR(Paragraph with )`code`TR( and )[TR(label)](https://example.com)TR( and )![TR(alt)](https://example.com/image.png)TR(.)\n\n"
                + "```bash\n"
                + "echo hello\n"
                + "```\n", callback.translatedText);

        assertEquals(List.of(
                "Title",
                "install package",
                "quote text",
                "Paragraph with | and |label| and |alt|."
        ), engine.inputs);
    }

    @Test
    public void translateMarkdown_splitsLargeParagraphsIntoMultipleChunks() {
        MarkdownStructureTranslator structureTranslator = new MarkdownStructureTranslator(new RecordingTranslationEngine(), 20);
        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        TestTranslationCallback callback = new TestTranslationCallback();

        structureTranslator.translate(
                builder.build("alpha *beta* gamma **delta** epsilon zeta"),
                "en",
                "es",
                callback
        );

        assertEquals("TR(alpha )*TR(beta)*TR( gamma )**TR(delta)**TR( epsilon zeta)", callback.translatedText);
    }

    @Test
    public void translateMarkdown_propagatesTranslationEngineFailure() {
        DefaultMarkdownTranslator translator = new DefaultMarkdownTranslator(new FailingTranslationEngine());

        TestTranslationCallback callback = new TestTranslationCallback();
        translator.translateMarkdown("# Title", "en", "es", callback);

        assertTrue(callback.error instanceof IllegalStateException);
        assertEquals("boom", callback.error.getMessage());
    }

    @Test
    public void translateMarkdown_fallsBackWhenMarkersAreMissingFromChunkResponse() {
        DefaultMarkdownTranslator translator = new DefaultMarkdownTranslator(new MarkerStrippingTranslationEngine());

        String source = "# Title\n\n"
                + "- install package\n"
                + "> quote text\n\n"
                + "Paragraph with [label](https://example.com).\n";

        TestTranslationCallback callback = new TestTranslationCallback();
        translator.translateMarkdown(source, "en", "es", callback);

        assertEquals("# TR(Title)\n\n"
                + "- TR(install package)\n"
                + "> TR(quote text)\n\n"
                + "TR(Paragraph with )[TR(label)](https://example.com)TR(.)\n", callback.translatedText);
        assertEquals(null, callback.error);
    }

    private static class RecordingTranslationEngine implements TranslationEngine {
        private static final Pattern MARKER_PATTERN = Pattern.compile("@@MLMD_TOKEN_[^@]+@@");
        private final java.util.ArrayList<String> inputs = new java.util.ArrayList<>();

        @Override
        public void translate(String text, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
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
        public void translate(String text, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
            callback.onFailure(new IllegalStateException("boom"));
        }
    }

    private static final class MarkerStrippingTranslationEngine implements TranslationEngine {
        private static final Pattern MARKER_PATTERN = Pattern.compile("@@MLMD_TOKEN_[^@]+@@");

        @Override
        public void translate(String text, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
            if (text.contains("@@MLMD_TOKEN_")) {
                // Simulates real translator behavior that mutates or removes markers.
                String withoutMarkers = MARKER_PATTERN.matcher(text).replaceAll("");
                callback.onSuccess("TR(" + withoutMarkers + ")");
                return;
            }
            callback.onSuccess("TR(" + text + ")");
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
}
