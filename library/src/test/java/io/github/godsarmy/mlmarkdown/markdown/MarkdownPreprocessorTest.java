package io.github.godsarmy.mlmarkdown.markdown;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MarkdownPreprocessorTest {
    @Test
    public void normalizeCustomBlockTags_convertsBlockTagsToFencedCode() {
        MarkdownPreprocessor preprocessor = new MarkdownPreprocessor();
        String input = "before\n<block>line 1\nline 2</block>\nafter";

        String normalized = preprocessor.normalizeCustomBlockTags(input);

        assertEquals("before\n```\nline 1\nline 2\n```\nafter", normalized);
    }

    @Test
    public void normalizeLineEndings_convertsCarriageReturnVariants() {
        MarkdownPreprocessor preprocessor = new MarkdownPreprocessor();
        String input = "a\r\nb\rc\n";

        String normalized = preprocessor.normalizeLineEndings(input);

        assertEquals("a\nb\nc\n", normalized);
    }
}
