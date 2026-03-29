package io.github.godsarmy.mlmarkdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.github.godsarmy.mlmarkdown.api.ExplainMarkdownResult;
import io.github.godsarmy.mlmarkdown.api.MarkdownTranslator;
import io.github.godsarmy.mlmarkdown.api.TranslationCallback;
import io.github.godsarmy.mlmarkdown.markdown.ProcessingMode;
import java.io.Closeable;
import java.util.List;
import org.junit.Test;

public class MlKitMarkdownTranslatorTest {
    @Test
    public void translateMarkdown_delegatesToMarkdownTranslator() {
        FakeMarkdownTranslator markdownTranslator = new FakeMarkdownTranslator();
        MlKitMarkdownTranslator facade =
                new MlKitMarkdownTranslator(markdownTranslator, new FakeCloseable());
        TestTranslationCallback callback = new TestTranslationCallback();

        facade.translateMarkdown("# Hello", "en", "es", callback);

        assertEquals("# Hello", markdownTranslator.markdown);
        assertEquals("en", markdownTranslator.sourceLanguage);
        assertEquals("es", markdownTranslator.targetLanguage);
        assertEquals("OK:# Hello", callback.translatedText);
    }

    @Test
    public void close_closesUnderlyingResource() {
        FakeCloseable closeable = new FakeCloseable();
        MlKitMarkdownTranslator facade =
                new MlKitMarkdownTranslator(new FakeMarkdownTranslator(), closeable);

        facade.close();

        assertTrue(closeable.closed);
    }

    @Test
    public void explainMarkdown_delegatesToMarkdownTranslator() {
        FakeMarkdownTranslator markdownTranslator = new FakeMarkdownTranslator();
        MlKitMarkdownTranslator facade =
                new MlKitMarkdownTranslator(markdownTranslator, new FakeCloseable());

        ExplainMarkdownResult result = facade.explainMarkdown("# Hello");

        assertNotNull(result);
        assertEquals("# Hello", markdownTranslator.explainedMarkdown);
        assertEquals(ProcessingMode.AST_TOKEN_STREAM, result.getProcessingMode());
    }

    private static final class FakeMarkdownTranslator implements MarkdownTranslator {
        private String markdown;
        private String sourceLanguage;
        private String targetLanguage;
        private String explainedMarkdown;

        @Override
        public void translateMarkdown(
                String markdown,
                String sourceLanguage,
                String targetLanguage,
                TranslationCallback callback) {
            this.markdown = markdown;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            callback.onSuccess("OK:" + markdown);
        }

        @Override
        public ExplainMarkdownResult explainMarkdown(String markdown) {
            this.explainedMarkdown = markdown;
            return new ExplainMarkdownResult(
                    ProcessingMode.AST_TOKEN_STREAM, markdown, List.of(), List.of(), List.of());
        }
    }

    private static final class FakeCloseable implements Closeable {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class TestTranslationCallback implements TranslationCallback {
        private String translatedText;

        @Override
        public void onSuccess(String translatedText) {
            this.translatedText = translatedText;
        }

        @Override
        public void onFailure(Exception error) {
            throw new AssertionError(error);
        }
    }
}
