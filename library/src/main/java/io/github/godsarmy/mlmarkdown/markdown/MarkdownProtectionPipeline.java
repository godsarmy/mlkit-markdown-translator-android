package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.model.ProtectedSegment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownProtectionPipeline {
    private static final Pattern PROTECTED_PATTERN = Pattern.compile(
            "(?s)```[\\s\\S]*?```|~~~[\\s\\S]*?~~~|`[^`\\n]+`|!\\[[^\\]]*]\\([^\\)\\n]+\\)|\\[[^\\]]+]\\([^\\)\\n]+\\)|<https?://[^>\\s]+>"
    );

    public String protect(String markdown, MarkdownTokenStore tokenStore) {
        Matcher matcher = PROTECTED_PATTERN.matcher(markdown);
        StringBuffer buffer = new StringBuffer();
        int tokenIndex = 1;

        while (matcher.find()) {
            String matched = matcher.group();
            String token = "__MLMD_PROTECTED_" + tokenIndex++ + "__";
            tokenStore.add(new ProtectedSegment(token, matched));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(token));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
