package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MarkdownGoldenFixtureTest {
    @Test
    public void fixture_astSource_parsesAndReconstructsExpectedMarkdownAfterMockTranslation() {
        String input = readFixture("fixtures/ast-preservation/input.md");
        String expected = readFixture("fixtures/ast-preservation/expected-after-mock-translation.md");

        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        TokenizedMarkdownDocument document = builder.build(input);

        assertEquals(input, document.reconstruct());
        assertTrue(document.getTokens().stream().anyMatch(token -> token.getType() == MarkdownTokenType.TRANSLATABLE));
        assertTrue(document.getTokens().stream().anyMatch(token -> token.getType() == MarkdownTokenType.PROTECTED));

        Map<String, String> translatedByToken = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : document.translatableTokenMap().entrySet()) {
            translatedByToken.put(entry.getKey(), "[[TRANSLATED:" + entry.getValue() + "]]");
        }

        String reconstructed = document.reconstructWithTranslations(translatedByToken);
        assertEquals(expected, reconstructed);
    }

    @Test
    public void fixture_tableAstSource_preservesTableDelimitersAfterMockTranslation() {
        String input = readFixture("fixtures/table-ast-preservation/input.md");
        String expected = readFixture("fixtures/table-ast-preservation/expected-after-mock-translation.md");

        AstTokenModelBuilder builder = new AstTokenModelBuilder();
        TokenizedMarkdownDocument document = builder.build(input);

        Map<String, String> translatedByToken = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : document.translatableTokenMap().entrySet()) {
            translatedByToken.put(entry.getKey(), "[[TR:" + entry.getValue() + "]]");
        }

        String reconstructed = document.reconstructWithTranslations(translatedByToken);
        assertEquals(expected, reconstructed);
        assertTrue(reconstructed.contains("| :--- | ---: |"));
    }

    @Test
    public void fixture_fallbackProtection_roundTripsProtectedSegmentsAfterMockTranslation() {
        String input = readFixture("fixtures/fallback-protection/input.md");
        String expected = readFixture("fixtures/fallback-protection/expected-restored-after-mock-translation.md");

        HybridMarkdownPreparationService service = new HybridMarkdownPreparationService() {
            @Override
            TokenizedMarkdownDocument buildTokenModel(String markdown) {
                throw new IllegalStateException("forced AST failure for fixture");
            }
        };

        MarkdownPreparationResult result = service.prepare(input);
        assertEquals(ProcessingMode.REGEX_FALLBACK, result.getMode());
        assertFalse(result.getTokenStore().getAll().isEmpty());

        String translated = "[[TRANSLATED:" + result.getMarkdownForTranslation() + "]]";
        String restored = new MarkdownRestorer().restore(translated, result.getTokenStore());
        assertEquals(expected.stripTrailing(), restored.stripTrailing());
    }

    @Test
    public void fixture_fallbackDisabled_skipsProtectionAndLeavesNoTokens() {
        String input = readFixture("fixtures/fallback-protection/input.md");
        HybridMarkdownPreparationService service = new HybridMarkdownPreparationService(
                new MarkdownTranslationOptions.Builder()
                        .setEnableRegexFallbackProtection(false)
                        .build()
        ) {
            @Override
            TokenizedMarkdownDocument buildTokenModel(String markdown) {
                throw new IllegalStateException("forced AST failure for fixture");
            }
        };

        MarkdownPreparationResult result = service.prepare(input);
        assertEquals(ProcessingMode.REGEX_FALLBACK, result.getMode());
        assertEquals(input, result.getMarkdownForTranslation());
        assertTrue(result.getTokenStore().getAll().isEmpty());
    }

    private static String readFixture(String path) {
        try (InputStream stream = MarkdownGoldenFixtureTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing fixture: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read fixture: " + path, e);
        }
    }
}
