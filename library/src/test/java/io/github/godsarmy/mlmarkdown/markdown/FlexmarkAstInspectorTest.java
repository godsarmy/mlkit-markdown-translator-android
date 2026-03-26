package io.github.godsarmy.mlmarkdown.markdown;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertTrue;

public class FlexmarkAstInspectorTest {
    @Test
    public void inspectNodeTypes_detectsRequiredMarkdownNodeCategories() {
        FlexmarkAstInspector inspector = new FlexmarkAstInspector();

        String markdown = "# Heading\\n\\n"
                + "Paragraph with *emphasis* and **strong** plus `inline code` and [link text](https://example.com).\\n\\n"
                + "![alt text](https://example.com/image.png)\\n\\n"
                + "> blockquote content\\n\\n"
                + "- bullet item\\n"
                + "1. ordered item\\n\\n"
                + "```kotlin\\n"
                + "println(\\\"hello\\\")\\n"
                + "```\\n";

        Set<String> categories = inspector.inspectNodeTypes(markdown);

        assertTrue(categories.contains("heading"));
        assertTrue(categories.contains("paragraph"));
        assertTrue(categories.contains("text"));
        assertTrue(categories.contains("emphasis"));
        assertTrue(categories.contains("strong_emphasis"));
        assertTrue(categories.contains("code_span"));
        assertTrue(categories.contains("link"));
        assertTrue(categories.contains("image"));
        assertTrue(categories.contains("blockquote"));
        assertTrue(categories.contains("bullet_list"));
        assertTrue(categories.contains("ordered_list"));
        assertTrue(categories.contains("list_item"));
        assertTrue(categories.contains("fenced_code_block"));
    }
}
