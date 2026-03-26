package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.engine.TranslationEngine;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
                "Paragraph with ",
                " and ",
                "label",
                " and ",
                "alt",
                "."
        ), engine.inputs);
    }

    @Test
    public void translateMarkdown_propagatesTranslationEngineFailure() {
        DefaultMarkdownTranslator translator = new DefaultMarkdownTranslator(new FailingTranslationEngine());

        TestTranslationCallback callback = new TestTranslationCallback();
        translator.translateMarkdown("# Title", "en", "es", callback);

        assertTrue(callback.error instanceof IllegalStateException);
        assertEquals("boom", callback.error.getMessage());
    }

    private static final class RecordingTranslationEngine implements TranslationEngine {
        private final List<String> inputs = new ArrayList<>();

        @Override
        public void translate(String text, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
            inputs.add(text);
            callback.onSuccess("TR(" + text + ")");
        }
    }

    private static final class FailingTranslationEngine implements TranslationEngine {
        @Override
        public void translate(String text, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
            callback.onFailure(new IllegalStateException("boom"));
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
