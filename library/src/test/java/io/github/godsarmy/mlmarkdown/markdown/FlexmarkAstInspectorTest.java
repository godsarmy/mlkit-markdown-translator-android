package io.github.godsarmy.mlmarkdown.markdown;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertTrue;

public class FlexmarkAstInspectorTest {
    @Test
    public void inspectNodeTypes_detectsRequiredMarkdownNodeCategories() {
        FlexmarkAstInspector inspector = new FlexmarkAstInspector();

        String markdown = "# Heading\n\n"
                + "Paragraph with *emphasis* and **strong** plus `inline code` and [link text](https://example.com).\n\n"
                + "![alt text](https://example.com/image.png)\n\n"
                + "> blockquote content\n\n"
                + "- bullet item\n"
                + "1. ordered item\n\n"
                + "```kotlin\n"
                + "println(\"hello\")\n"
                + "```\n";

        Set<String> categories = inspector.inspectNodeTypes(markdown);

        String message = "categories=" + categories;
        assertTrue(message, categories.contains("heading"));
        assertTrue(message, categories.contains("paragraph"));
        assertTrue(message, categories.contains("text"));
        assertTrue(message, categories.contains("emphasis"));
        assertTrue(message, categories.contains("strong_emphasis"));
        assertTrue(message, categories.contains("code_span"));
        assertTrue(message, categories.contains("link"));
        assertTrue(message, categories.contains("image"));
        assertTrue(message, categories.contains("blockquote"));
        assertTrue(message, categories.contains("bullet_list"));
        assertTrue(message, categories.contains("ordered_list"));
        assertTrue(message, categories.contains("list_item"));
        assertTrue(message, categories.contains("fenced_code_block"));
    }
}
