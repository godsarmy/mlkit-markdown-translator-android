package io.github.godsarmy.mlmarkdown.markdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MarkdownProtectionPipelineTest {
    @Test
    public void protect_replacesMarkdownSensitiveSegmentsWithTokens() {
        MarkdownProtectionPipeline pipeline = new MarkdownProtectionPipeline();
        MarkdownTokenStore tokenStore = new MarkdownTokenStore();

        String input =
                "Here is `inline` code and [label](https://example.com) with ![img](https://example.com/x.png) "
                        + "and <https://example.com>.\n\n```kotlin\nprintln(\"hi\")\n```";

        String protectedMarkdown = pipeline.protect(input, tokenStore);

        assertFalse(protectedMarkdown.contains("`inline`"));
        assertFalse(protectedMarkdown.contains("[label](https://example.com)"));
        assertFalse(protectedMarkdown.contains("![img](https://example.com/x.png)"));
        assertFalse(protectedMarkdown.contains("<https://example.com>"));
        assertFalse(protectedMarkdown.contains("```kotlin"));
        assertEquals(5, tokenStore.getAll().size());
    }

    @Test
    public void protectThenRestore_roundTripsOriginalMarkdown() {
        MarkdownProtectionPipeline pipeline = new MarkdownProtectionPipeline();
        MarkdownRestorer restorer = new MarkdownRestorer();
        MarkdownTokenStore tokenStore = new MarkdownTokenStore();

        String input =
                "before\n```bash\necho hi\n```\n`code` [link](https://example.com) ![image](https://example.com/i.png) <https://example.com>\nafter";

        String protectedMarkdown = pipeline.protect(input, tokenStore);
        String translated = "TR(" + protectedMarkdown + ")";
        String restored = restorer.restore(translated, tokenStore);

        assertTrue(restored.contains("```bash\necho hi\n```"));
        assertTrue(restored.contains("`code`"));
        assertTrue(restored.contains("[link](https://example.com)"));
        assertTrue(restored.contains("![image](https://example.com/i.png)"));
        assertTrue(restored.contains("<https://example.com>"));
    }

    @Test
    public void protect_replacesEscapedMarkdownCharactersWithTokens() {
        MarkdownProtectionPipeline pipeline = new MarkdownProtectionPipeline();
        MarkdownRestorer restorer = new MarkdownRestorer();
        MarkdownTokenStore tokenStore = new MarkdownTokenStore();

        String input = "Escaped \\*data\\*, \\#data, and \\[label\\] stay literal";

        String protectedMarkdown = pipeline.protect(input, tokenStore);

        assertFalse(protectedMarkdown.contains("\\*"));
        assertFalse(protectedMarkdown.contains("\\#"));
        assertFalse(protectedMarkdown.contains("\\["));
        assertFalse(protectedMarkdown.contains("\\]"));
        assertEquals(5, tokenStore.getAll().size());
        assertEquals(input, restorer.restore(protectedMarkdown, tokenStore));
    }

    @Test
    public void protect_usesConfiguredEscapedMarkdownCharacters() {
        MarkdownProtectionPipeline pipeline = new MarkdownProtectionPipeline("#[]");
        MarkdownRestorer restorer = new MarkdownRestorer();
        MarkdownTokenStore tokenStore = new MarkdownTokenStore();

        String input = "Escaped \\*data\\*, \\#data, and \\[label\\] stay literal";

        String protectedMarkdown = pipeline.protect(input, tokenStore);

        assertTrue(protectedMarkdown.contains("\\*"));
        assertFalse(protectedMarkdown.contains("\\#"));
        assertFalse(protectedMarkdown.contains("\\["));
        assertFalse(protectedMarkdown.contains("\\]"));
        assertEquals(3, tokenStore.getAll().size());
        assertEquals(input, restorer.restore(protectedMarkdown, tokenStore));
    }

    @Test
    public void protect_preservesWholeTableBlockInFallbackMode() {
        MarkdownProtectionPipeline pipeline = new MarkdownProtectionPipeline();
        MarkdownRestorer restorer = new MarkdownRestorer();
        MarkdownTokenStore tokenStore = new MarkdownTokenStore();

        String input =
                "| Name | Notes |\n"
                        + "| --- | --- |\n"
                        + "| Alice | value |\n"
                        + "| Bob | value |\n";

        String protectedMarkdown = pipeline.protect(input, tokenStore);

        assertFalse(protectedMarkdown.contains("| Name | Notes |"));
        assertEquals(1, tokenStore.getAll().size());

        String restored = restorer.restore("TR(" + protectedMarkdown + ")", tokenStore);
        assertTrue(restored.contains("| --- | --- |"));
        assertTrue(restored.contains("| Alice | value |"));
        assertTrue(restored.contains("| Bob | value |"));
    }
}
