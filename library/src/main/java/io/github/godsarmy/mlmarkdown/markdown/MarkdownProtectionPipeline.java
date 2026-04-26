package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.model.ProtectedSegment;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownProtectionPipeline {
    private static final Pattern TABLE_BLOCK_PATTERN =
            Pattern.compile(
                    "(?m)(^\\|?.*\\|.*\\R^\\|?(?:\\s*:?-{3,}:?\\s*\\|)+\\s*\\R(?:^\\|?.*\\|.*(?:\\R|$))+)");

    private static final Pattern PROTECTED_PATTERN =
            Pattern.compile(
                    "(?s)```[\\s\\S]*?```|~~~[\\s\\S]*?~~~|`[^`\\n]+`|!\\[[^\\]]*]\\([^\\)\\n]+\\)|\\[[^\\]]+]\\([^\\)\\n]+\\)|<https?://[^>\\s]+>");

    private final String escapedMarkdownCharactersToProtect;

    public MarkdownProtectionPipeline() {
        this(MarkdownTranslationOptions.DEFAULT_ESCAPED_MARKDOWN_CHARACTERS);
    }

    public MarkdownProtectionPipeline(String escapedMarkdownCharactersToProtect) {
        this.escapedMarkdownCharactersToProtect =
                Objects.requireNonNull(
                        escapedMarkdownCharactersToProtect,
                        "escapedMarkdownCharactersToProtect == null");
    }

    public String protect(String markdown, MarkdownTokenStore tokenStore) {
        ProtectedText tablesProtected =
                protectByPattern(markdown, TABLE_BLOCK_PATTERN, tokenStore, 1);
        ProtectedText inlineProtected =
                protectByPattern(
                        tablesProtected.text,
                        PROTECTED_PATTERN,
                        tokenStore,
                        tablesProtected.nextTokenIndex);
        ProtectedText escapesProtected =
                protectEscapedMarkdownCharacters(
                        inlineProtected.text,
                        tokenStore,
                        inlineProtected.nextTokenIndex,
                        escapedMarkdownCharactersToProtect);

        return escapesProtected.text;
    }

    private static ProtectedText protectByPattern(
            String markdown, Pattern pattern, MarkdownTokenStore tokenStore, int startIndex) {
        Matcher matcher = pattern.matcher(markdown);
        StringBuffer buffer = new StringBuffer();
        int tokenIndex = startIndex;

        while (matcher.find()) {
            String matched = matcher.group();
            String token = "__MLMD_PROTECTED_" + tokenIndex++ + "__";
            tokenStore.add(new ProtectedSegment(token, matched));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(token));
        }

        matcher.appendTail(buffer);
        return new ProtectedText(buffer.toString(), tokenIndex);
    }

    private static ProtectedText protectEscapedMarkdownCharacters(
            String markdown,
            MarkdownTokenStore tokenStore,
            int startIndex,
            String escapedMarkdownCharactersToProtect) {
        StringBuilder protectedMarkdown = new StringBuilder(markdown.length());
        int tokenIndex = startIndex;

        for (int i = 0; i < markdown.length(); i++) {
            if (i < markdown.length() - 1
                    && markdown.charAt(i) == '\\'
                    && escapedMarkdownCharactersToProtect.indexOf(markdown.charAt(i + 1)) >= 0) {
                String matched = markdown.substring(i, i + 2);
                String token = "__MLMD_PROTECTED_" + tokenIndex++ + "__";
                tokenStore.add(new ProtectedSegment(token, matched));
                protectedMarkdown.append(token);
                i++;
                continue;
            }

            protectedMarkdown.append(markdown.charAt(i));
        }

        return new ProtectedText(protectedMarkdown.toString(), tokenIndex);
    }

    private static final class ProtectedText {
        private final String text;
        private final int nextTokenIndex;

        private ProtectedText(String text, int nextTokenIndex) {
            this.text = text;
            this.nextTokenIndex = nextTokenIndex;
        }
    }
}
