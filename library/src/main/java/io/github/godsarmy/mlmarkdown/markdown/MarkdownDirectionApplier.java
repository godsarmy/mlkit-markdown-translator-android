package io.github.godsarmy.mlmarkdown.markdown;

import androidx.annotation.Nullable;
import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class MarkdownDirectionApplier {
    private static final char LRI = '\u2066';
    private static final char RLI = '\u2067';
    private static final char PDI = '\u2069';
    private static final Set<String> RTL_LANGUAGE_CODES =
            new HashSet<>(
                    Arrays.asList("ar", "dv", "fa", "he", "iw", "ps", "sd", "ug", "ur", "yi"));

    @Nullable
    Direction resolveDirection(
            String targetLanguage, MarkdownTranslationOptions.OutputDirectionMode mode) {
        switch (mode) {
            case FORCE_LTR:
                return Direction.LTR;
            case FORCE_RTL:
                return Direction.RTL;
            case AUTO_FROM_TARGET_LANGUAGE:
                String languageCode = normalizePrimaryLanguage(targetLanguage);
                if (languageCode == null) {
                    return null;
                }
                return RTL_LANGUAGE_CODES.contains(languageCode) ? Direction.RTL : Direction.LTR;
            case PRESERVE:
            default:
                return null;
        }
    }

    String applyToText(String value, @Nullable Direction direction) {
        if (direction == null || value == null || value.isEmpty()) {
            return value;
        }

        String leadingWhitespace = leadingWhitespace(value);
        String trailingWhitespace = trailingWhitespace(value);
        int contentStart = leadingWhitespace.length();
        int contentEnd = value.length() - trailingWhitespace.length();
        if (contentStart >= contentEnd) {
            return value;
        }

        char isolate = direction == Direction.RTL ? RLI : LRI;
        return leadingWhitespace
                + isolate
                + value.substring(contentStart, contentEnd)
                + PDI
                + trailingWhitespace;
    }

    @Nullable
    String normalizePrimaryLanguage(String languageCode) {
        String normalized = languageCode.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        int separatorIndex = normalized.indexOf('-');
        if (separatorIndex < 0) {
            separatorIndex = normalized.indexOf('_');
        }
        if (separatorIndex > 0) {
            normalized = normalized.substring(0, separatorIndex);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String leadingWhitespace(String value) {
        int index = 0;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return value.substring(0, index);
    }

    private static String trailingWhitespace(String value) {
        int index = value.length() - 1;
        while (index >= 0 && Character.isWhitespace(value.charAt(index))) {
            index--;
        }
        return value.substring(index + 1);
    }

    enum Direction {
        LTR,
        RTL
    }
}
