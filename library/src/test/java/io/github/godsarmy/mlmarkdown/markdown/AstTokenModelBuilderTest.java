package io.github.godsarmy.mlmarkdown.markdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class AstTokenModelBuilderTest {
    @Test
    public void build_preservesExactOrderingAndReconstructsOriginalMarkdown() {
        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        String markdown =
                "# Title\n\n"
                        + "Paragraph with [link label](https://example.com) and `inline code`.\n\n"
                        + "- item one\n"
                        + "1. item two\n\n"
                        + "> quote\n\n"
                        + "```java\n"
                        + "System.out.println(\"ok\");\n"
                        + "```\n";

        TokenizedMarkdownDocument document = builder.build(markdown);

        assertEquals(markdown, document.reconstruct());
    }

    @Test
    public void build_marksStructuralProtectedAndTranslatableTokens() {
        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        String markdown =
                "# Title\n\n"
                        + "Text with `code` and <https://example.com>\n\n"
                        + "```kotlin\n"
                        + "println(\"hi\")\n"
                        + "```\n";

        TokenizedMarkdownDocument document = builder.build(markdown);
        List<MarkdownToken> tokens = document.getTokens();

        assertTrue(
                tokens.stream().anyMatch(token -> token.getType() == MarkdownTokenType.STRUCTURAL));
        assertTrue(
                tokens.stream()
                        .anyMatch(token -> token.getType() == MarkdownTokenType.TRANSLATABLE));
        assertTrue(
                tokens.stream().anyMatch(token -> token.getType() == MarkdownTokenType.PROTECTED));

        assertTrue(
                tokens.stream()
                        .anyMatch(
                                token ->
                                        token.getType() == MarkdownTokenType.PROTECTED
                                                && token.getValue().contains("`code`")));
        assertTrue(
                tokens.stream()
                        .anyMatch(
                                token ->
                                        token.getType() == MarkdownTokenType.PROTECTED
                                                && token.getValue().contains("```kotlin")));

        List<String> translatableTokenIds =
                tokens.stream()
                        .filter(token -> token.getType() == MarkdownTokenType.TRANSLATABLE)
                        .map(MarkdownToken::getTokenId)
                        .collect(Collectors.toList());
        assertTrue(!translatableTokenIds.isEmpty());
        for (int i = 0; i < translatableTokenIds.size(); i++) {
            assertEquals("T" + (i + 1), translatableTokenIds.get(i));
        }
    }

    @Test
    public void build_allowsAutolinksToBeTranslatable_whenProtectionDisabled() {
        AstTokenModelBuilder builder = new AstTokenModelBuilder(false);
        String markdown = "Visit <https://example.com> now";

        TokenizedMarkdownDocument document = builder.build(markdown);

        assertTrue(
                document.getTokens().stream()
                        .noneMatch(
                                token ->
                                        token.getType() == MarkdownTokenType.PROTECTED
                                                && token.getValue()
                                                        .contains("https://example.com")));
    }

    @Test
    public void build_protectsEscapedMarkdownCharactersFromTranslation() {
        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        String markdown = "Escaped \\*data\\*, \\#data, and \\[label\\] stay literal";

        TokenizedMarkdownDocument document = builder.build(markdown);

        assertEquals(markdown, document.reconstruct());
        assertProtectedToken(document, "\\*");
        assertProtectedToken(document, "\\#");
        assertProtectedToken(document, "\\[");
        assertProtectedToken(document, "\\]");
        assertFalse(
                document.translatableTokenMap().values().stream()
                        .anyMatch(
                                value ->
                                        value.contains("\\*")
                                                || value.contains("\\#")
                                                || value.contains("\\[")
                                                || value.contains("\\]")));
    }

    @Test
    public void build_usesConfiguredEscapedMarkdownCharacters() {
        AstTokenModelBuilder builder = new AstTokenModelBuilder(true, "#[]");
        String markdown = "Escaped \\*data\\*, \\#data, and \\[label\\] stay literal";

        TokenizedMarkdownDocument document = builder.build(markdown);

        assertProtectedToken(document, "\\#");
        assertProtectedToken(document, "\\[");
        assertProtectedToken(document, "\\]");
        assertFalse(
                document.getTokens().stream()
                        .anyMatch(
                                token ->
                                        token.getType() == MarkdownTokenType.PROTECTED
                                                && token.getValue().equals("\\*")));
        assertTrue(
                document.translatableTokenMap().values().stream()
                        .anyMatch(value -> value.contains("\\*")));
    }

    @Test
    public void build_preservesTableStructureWhileAllowingCellTextTranslation() {
        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        String markdown =
                "| Name | Notes |\n"
                        + "| :--- | ---: |\n"
                        + "| Alice | `code` and [link](https://example.com) |\n"
                        + "| Bob | plain text |\n";

        TokenizedMarkdownDocument document = builder.build(markdown);
        Map<String, String> translations =
                document.translatableTokenMap().entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> "TR(" + entry.getValue() + ")",
                                        (a, b) -> a,
                                        java.util.LinkedHashMap::new));

        String reconstructed = document.reconstructWithTranslations(translations);

        assertTrue(reconstructed.contains("| :--- | ---: |"));
        assertTrue(reconstructed.contains("| TR(Name) | TR(Notes) |"));
        assertTrue(reconstructed.contains("| TR(Alice) |"));
        assertTrue(reconstructed.contains("`code`"));
        assertTrue(reconstructed.contains("[TR(link)](https://example.com)"));
        assertTrue(reconstructed.contains("| TR(Bob) | TR(plain text) |"));
    }

    @Test
    public void build_preservesSetextHardBreakHrReferenceCodeAndEscapedCharacters() {
        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        String markdown =
                "Setext Heading H1\n"
                        + "=================\n\n"
                        + "Setext Heading H2\n"
                        + "-----------------\n\n"
                        + "Paragraph with hard break  \n"
                        + "continuation line\n\n"
                        + "***\n"
                        + "---\n"
                        + "___\n\n"
                        + "Reference [text label][ref]\n\n"
                        + "[ref]: https://example.com \"Example Title\"\n\n"
                        + "    indented code block line\n\n"
                        + "Escaped literal \\* and \\[ should stay literal\n";

        TokenizedMarkdownDocument document = builder.build(markdown);

        assertEquals(markdown, document.reconstruct());
    }

    private static void assertProtectedToken(TokenizedMarkdownDocument document, String value) {
        assertTrue(
                document.getTokens().stream()
                        .anyMatch(
                                token ->
                                        token.getType() == MarkdownTokenType.PROTECTED
                                                && token.getValue().equals(value)));
    }
}
