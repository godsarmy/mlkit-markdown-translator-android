package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.model.ProtectedSegment;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownProtectionPipeline {
    private static final Pattern TABLE_BLOCK_PATTERN =
            Pattern.compile(
                    "(?m)(^\\|?.*\\|.*\\R^\\|?(?:\\s*:?-{3,}:?\\s*\\|)+\\s*\\R(?:^\\|?.*\\|.*(?:\\R|$))+)");

    private static final Pattern PROTECTED_PATTERN =
            Pattern.compile(
                    "(?s)```[\\s\\S]*?```|~~~[\\s\\S]*?~~~|`[^`\\n]+`|!\\[[^\\]]*]\\([^\\)\\n]+\\)|\\[[^\\]]+]\\([^\\)\\n]+\\)|<https?://[^>\\s]+>");

    public String protect(String markdown, MarkdownTokenStore tokenStore) {
        ProtectedText tablesProtected =
                protectByPattern(markdown, TABLE_BLOCK_PATTERN, tokenStore, 1);
        ProtectedText inlineProtected =
                protectByPattern(
                        tablesProtected.text,
                        PROTECTED_PATTERN,
                        tokenStore,
                        tablesProtected.nextTokenIndex);

        return inlineProtected.text;
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

    private static final class ProtectedText {
        private final String text;
        private final int nextTokenIndex;

        private ProtectedText(String text, int nextTokenIndex) {
            this.text = text;
            this.nextTokenIndex = nextTokenIndex;
        }
    }
}
