package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HybridMarkdownPreparationServiceTest {
    @Test
    public void prepare_usesAstTokenStreamMode_whenParsingSucceeds() {
        HybridMarkdownPreparationService service = new HybridMarkdownPreparationService();

        MarkdownPreparationResult result = service.prepare("# title\n\nparagraph");

        assertEquals(ProcessingMode.AST_TOKEN_STREAM, result.getMode());
        assertNotNull(result.getTokenizedDocument());
    }

    @Test
    public void prepare_usesRegexFallbackMode_whenAstBuilderThrows() {
        HybridMarkdownPreparationService service = new HybridMarkdownPreparationService() {
            @Override
            TokenizedMarkdownDocument buildTokenModel(String markdown) {
                throw new IllegalStateException("forced failure");
            }
        };

        MarkdownPreparationResult result = service.prepare("# title");

        assertEquals(ProcessingMode.REGEX_FALLBACK, result.getMode());
        assertNotNull(result.getTokenStore());
    }

    @Test
    public void prepare_skipsBlockTagNormalization_whenOptionDisabled() {
        HybridMarkdownPreparationService service = new HybridMarkdownPreparationService(
                new MarkdownTranslationOptions.Builder()
                        .setNormalizeCustomBlockTags(false)
                        .build()
        );

        String source = "before\n<block>line 1\nline 2</block>\nafter";
        MarkdownPreparationResult result = service.prepare(source);

        assertEquals(source, result.getMarkdownForTranslation());
    }
}
