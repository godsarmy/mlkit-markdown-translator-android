package io.github.godsarmy.mlmarkdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.github.godsarmy.mlmarkdown.api.TranslationTimingListener;
import io.github.godsarmy.mlmarkdown.api.TranslationTimingReport;
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
        assertTrue(options.enableRegexFallbackProtection());
        assertTrue(options.preserveWhitespaceAroundProtectedSegments());
        assertEquals(
                MarkdownTranslationOptions.DEFAULT_ESCAPED_MARKDOWN_CHARACTERS,
                options.escapedMarkdownCharactersToProtect());
        assertFalse(options.escapedMarkdownCharactersToProtect().contains("_"));
        assertFalse(options.escapedMarkdownCharactersToProtect().contains("-"));
        assertFalse(options.escapedMarkdownCharactersToProtect().contains("+"));
        assertEquals(MarkdownTranslationOptions.DEFAULT_TOKEN_MARKER, options.tokenMarker());
        assertNull(options.translationTimingListener());
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
                        .setEnableRegexFallbackProtection(false)
                        .setPreserveWhitespaceAroundProtectedSegments(false)
                        .setEscapedMarkdownCharactersToProtect("*#")
                        .setTokenMarker("##")
                        .build();

        assertFalse(options.preserveNewlines());
        assertFalse(options.preserveListPrefixes());
        assertFalse(options.preserveBlockquotes());
        assertFalse(options.normalizeCustomBlockTags());
        assertFalse(options.protectAutolinks());
        assertFalse(options.enableRegexFallbackProtection());
        assertFalse(options.preserveWhitespaceAroundProtectedSegments());
        assertEquals("*#", options.escapedMarkdownCharactersToProtect());
        assertEquals("##", options.tokenMarker());
        assertNull(options.translationTimingListener());
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
    public void builder_allowsSettingTimingListener() {
        TranslationTimingListener listener = new NoOpTimingListener();
        MarkdownTranslationOptions options =
                new MarkdownTranslationOptions.Builder()
                        .setTranslationTimingListener(listener)
                        .build();

        assertSame(listener, options.translationTimingListener());
    }

    private static final class NoOpTimingListener implements TranslationTimingListener {
        @Override
        public void onCompleted(TranslationTimingReport report) {
            // no-op
        }
    }
}
