package io.github.godsarmy.mlmarkdown.markdown;

public class MarkdownPreprocessor {
    public String normalizeLineEndings(String markdown) {
        return markdown
                .replace("\r\n", "\n")
                .replace("\r", "\n");
    }
}
