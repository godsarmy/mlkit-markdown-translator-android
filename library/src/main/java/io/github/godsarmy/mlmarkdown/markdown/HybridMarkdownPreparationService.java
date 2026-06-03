package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.MarkdownTranslationOptions;
import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;

/** Markdown preparation service backed by Flexmark AST tokenization. */
public class HybridMarkdownPreparationService {
    private final MarkdownPreprocessor preprocessor;
    private final AstTokenModelBuilder tokenModelBuilder;
    private final MarkdownTranslationOptions options;

    public HybridMarkdownPreparationService() {
        this(MarkdownTranslationOptions.defaults());
    }

    public HybridMarkdownPreparationService(MarkdownTranslationOptions options) {
        this(
                new MarkdownPreprocessor(),
                new AstTokenModelBuilder(
                        options.protectAutolinks(), options.escapedMarkdownCharactersToProtect()),
                options);
    }

    public HybridMarkdownPreparationService(
            MarkdownPreprocessor preprocessor,
            AstTokenModelBuilder tokenModelBuilder,
            MarkdownTranslationOptions options) {
        this.preprocessor = preprocessor;
        this.tokenModelBuilder = tokenModelBuilder;
        this.options = options;
    }

    public MarkdownPreparationResult prepare(String markdown) {
        String normalized = preprocessor.normalizeLineEndings(markdown);
        String normalizedBlocks =
                options.normalizeCustomBlockTags()
                        ? preprocessor.normalizeCustomBlockTags(normalized)
                        : normalized;

        TokenizedMarkdownDocument tokenizedDocument = buildTokenModel(normalizedBlocks);
        return new MarkdownPreparationResult(
                ProcessingMode.AST_TOKEN_STREAM,
                tokenizedDocument.reconstruct(),
                tokenizedDocument);
    }

    TokenizedMarkdownDocument buildTokenModel(String markdown) {
        return tokenModelBuilder.build(markdown);
    }
}
