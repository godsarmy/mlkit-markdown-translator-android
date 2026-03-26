package io.github.godsarmy.mlmarkdown.markdown;

import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;

/**
 * Step 6E hybrid mode:
 * - Primary: AST/token-stream preparation via Flexmark.
 * - Fallback: regex protection pipeline when AST preparation fails.
 */
public class HybridMarkdownPreparationService {
    private final MarkdownPreprocessor preprocessor;
    private final AstTokenModelBuilder tokenModelBuilder;
    private final MarkdownProtectionPipeline protectionPipeline;

    public HybridMarkdownPreparationService() {
        this(new MarkdownPreprocessor(), new AstTokenModelBuilder(), new MarkdownProtectionPipeline());
    }

    public HybridMarkdownPreparationService(
            MarkdownPreprocessor preprocessor,
            AstTokenModelBuilder tokenModelBuilder,
            MarkdownProtectionPipeline protectionPipeline
    ) {
        this.preprocessor = preprocessor;
        this.tokenModelBuilder = tokenModelBuilder;
        this.protectionPipeline = protectionPipeline;
    }

    public MarkdownPreparationResult prepare(String markdown) {
        String normalized = preprocessor.normalizeLineEndings(markdown);
        String normalizedBlocks = preprocessor.normalizeCustomBlockTags(normalized);

        try {
            TokenizedMarkdownDocument tokenizedDocument = buildTokenModel(normalizedBlocks);
            return new MarkdownPreparationResult(
                    ProcessingMode.AST_TOKEN_STREAM,
                    tokenizedDocument.reconstruct(),
                    new MarkdownTokenStore(),
                    tokenizedDocument
            );
        } catch (RuntimeException parseError) {
            MarkdownTokenStore tokenStore = new MarkdownTokenStore();
            String protectedMarkdown = protectionPipeline.protect(normalizedBlocks, tokenStore);
            return new MarkdownPreparationResult(
                    ProcessingMode.REGEX_FALLBACK,
                    protectedMarkdown,
                    tokenStore,
                    null
            );
        }
    }

    TokenizedMarkdownDocument buildTokenModel(String markdown) {
        return tokenModelBuilder.build(markdown);
    }
}
