package io.github.godsarmy.mlmarkdown.markdown;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownPreprocessor {
    private static final Pattern BLOCK_TAG_PATTERN = Pattern.compile("<block>(.*?)</block>", Pattern.DOTALL);

    public String normalizeLineEndings(String markdown) {
        return markdown
                .replace("\r\n", "\n")
                .replace("\r", "\n");
    }

    public String normalizeCustomBlockTags(String markdown) {
        Matcher matcher = BLOCK_TAG_PATTERN.matcher(markdown);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String codeBody = matcher.group(1);
            String replacement = "```\n" + codeBody + "\n```";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
