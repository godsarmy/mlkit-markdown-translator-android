package io.github.godsarmy.mlmarkdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.github.godsarmy.mlmarkdown.api.TranslationMetricsListener;
import io.github.godsarmy.mlmarkdown.api.TranslationMetricsReport;
import org.junit.Test;

public class MarkdownTranslationOptionsTest {
    @Test
    public void defaults_enableAllOptions() {
        MarkdownTranslationOptions options = MarkdownTranslationOptions.defaults();

        assertTrue(options.preserveNewlines());
        assertTrue(options.preserveListPrefixes());
        assertTrue(options.preserveBlockquotes());
        assertTrue(options.normalizeCustomBlockTags());
        assertTrue(options.protectAutolinks());
        assertTrue(options.preserveWhitespaceAroundProtectedSegments());
        assertEquals(
                MarkdownTranslationOptions.DEFAULT_ESCAPED_MARKDOWN_CHARACTERS,
                options.escapedMarkdownCharactersToProtect());
        assertFalse(options.escapedMarkdownCharactersToProtect().contains("_"));
        assertFalse(options.escapedMarkdownCharactersToProtect().contains("{"));
        assertFalse(options.escapedMarkdownCharactersToProtect().contains("}"));
        assertTrue(options.escapedMarkdownCharactersToProtect().contains("-"));
        assertTrue(options.escapedMarkdownCharactersToProtect().contains("+"));
        assertEquals(MarkdownTranslationOptions.DEFAULT_TOKEN_MARKER, options.tokenMarker());
        assertNull(options.translationMetricsListener());
    }

    @Test
    public void builder_allowsDisablingOptions() {
        MarkdownTranslationOptions options =
                new MarkdownTranslationOptions.Builder()
                        .setPreserveNewlines(false)
                        .setPreserveListPrefixes(false)
                        .setPreserveBlockquotes(false)
                        .setNormalizeCustomBlockTags(false)
                        .setProtectAutolinks(false)
                        .setPreserveWhitespaceAroundProtectedSegments(false)
                        .setEscapedMarkdownCharactersToProtect("*#")
                        .setTokenMarker("##")
                        .build();

        assertFalse(options.preserveNewlines());
        assertFalse(options.preserveListPrefixes());
        assertFalse(options.preserveBlockquotes());
        assertFalse(options.normalizeCustomBlockTags());
        assertFalse(options.protectAutolinks());
        assertFalse(options.preserveWhitespaceAroundProtectedSegments());
        assertEquals("*#", options.escapedMarkdownCharactersToProtect());
        assertEquals("##", options.tokenMarker());
        assertNull(options.translationMetricsListener());
    }

    @Test
    public void builder_rejectsEmptyTokenMarker() {
        try {
            new MarkdownTranslationOptions.Builder().setTokenMarker("");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals("tokenMarker must not be empty", expected.getMessage());
        }
    }

    @Test
    public void builder_allowsSettingMetricsListener() {
        TranslationMetricsListener listener = new NoOpMetricsListener();
        MarkdownTranslationOptions options =
                new MarkdownTranslationOptions.Builder()
                        .setTranslationMetricsListener(listener)
                        .build();

        assertSame(listener, options.translationMetricsListener());
    }

    private static final class NoOpMetricsListener implements TranslationMetricsListener {
        @Override
        public void onCompleted(TranslationMetricsReport report) {
            // no-op
        }
    }
}
