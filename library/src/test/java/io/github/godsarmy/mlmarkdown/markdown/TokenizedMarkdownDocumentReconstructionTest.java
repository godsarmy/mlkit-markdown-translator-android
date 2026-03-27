package io.github.godsarmy.mlmarkdown.markdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class TokenizedMarkdownDocumentReconstructionTest {
    @Test
    public void reconstructWithTranslations_preservesMarkdownStructureDelimiters() {
        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        String source =
                "# Title\n\n"
                        + "- install package\n"
                        + "> quote text\n\n"
                        + "inline `code` and [label](https://example.com) and ![alt](https://example.com/a.png)\n\n"
                        + "```bash\n"
                        + "echo hello\n"
                        + "```\n";

        TokenizedMarkdownDocument document = builder.build(source);
        Map<String, String> translations = new HashMap<>();
        for (Map.Entry<String, String> entry : document.translatableTokenMap().entrySet()) {
            translations.put(entry.getKey(), "TR(" + entry.getValue() + ")");
        }

        String reconstructed = document.reconstructWithTranslations(translations);

        assertTrue(reconstructed.contains("# TR(Title)"));
        assertTrue(reconstructed.contains("- TR(install package)"));
        assertTrue(reconstructed.contains("> TR(quote text)"));
        assertTrue(reconstructed.contains("`code`"));
        assertTrue(reconstructed.contains("[TR(label)](https://example.com)"));
        assertTrue(reconstructed.contains("![TR(alt)](https://example.com/a.png)"));
        assertTrue(reconstructed.contains("```bash\necho hello\n```"));
    }

    @Test
    public void reconstructWithTranslations_fallsBackToSourceWhenTokenTranslationMissing() {
        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        String source = "# Heading\n\nParagraph text\n";

        TokenizedMarkdownDocument document = builder.build(source);
        String reconstructed = document.reconstructWithTranslations(Map.of());

        assertEquals(source, reconstructed);
    }
}
