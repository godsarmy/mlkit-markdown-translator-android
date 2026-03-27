package io.github.godsarmy.mlmarkdown.markdown;

import com.vladsch.flexmark.ast.AutoLink;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.tables.TableSeparator;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import io.github.godsarmy.mlmarkdown.model.TokenizedMarkdownDocument;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/** Step 6C: Converts a Flexmark AST into an ordered token stream. */
public final class AstTokenModelBuilder {
    private final Parser parser;
    private final boolean protectAutolinks;

    public AstTokenModelBuilder() {
        this(true);
    }

    public AstTokenModelBuilder(boolean protectAutolinks) {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        this.parser = Parser.builder(options).build();
        this.protectAutolinks = protectAutolinks;
    }

    public TokenizedMarkdownDocument build(String markdown) {
        Node document = parser.parse(markdown);

        List<Span> translatableSpans = new ArrayList<>();
        List<Span> protectedSpans = new ArrayList<>();
        collectSpans(document, translatableSpans, protectedSpans, protectAutolinks);

        List<MarkdownToken> tokens = toTokenStream(markdown, translatableSpans, protectedSpans);
        assignTranslatableTokenIds(tokens);
        return new TokenizedMarkdownDocument(markdown, tokens);
    }

    private static void collectSpans(
            Node node,
            List<Span> translatableSpans,
            List<Span> protectedSpans,
            boolean protectAutolinks) {
        BasedSequence chars = node.getChars();
        int start = chars.getStartOffset();
        int end = chars.getEndOffset();
        if (start >= 0 && end > start) {
            if (node instanceof FencedCodeBlock
                    || node instanceof Code
                    || node instanceof HtmlBlock
                    || node instanceof HtmlInline
                    || node instanceof TableSeparator) {
                protectedSpans.add(new Span(start, end));
            } else if (protectAutolinks && node instanceof AutoLink) {
                protectedSpans.add(new Span(start, end));
            } else if (node instanceof Text) {
                translatableSpans.add(new Span(start, end));
            }
        }

        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            collectSpans(child, translatableSpans, protectedSpans, protectAutolinks);
        }
    }

    private static List<MarkdownToken> toTokenStream(
            String source, List<Span> translatableSpans, List<Span> protectedSpans) {
        TreeSet<Integer> boundaries = new TreeSet<>(Comparator.naturalOrder());
        boundaries.add(0);
        boundaries.add(source.length());
        addBoundaries(boundaries, translatableSpans);
        addBoundaries(boundaries, protectedSpans);

        List<MarkdownToken> tokens = new ArrayList<>();
        Integer[] points = boundaries.toArray(new Integer[0]);

        for (int i = 0; i < points.length - 1; i++) {
            int start = points[i];
            int end = points[i + 1];
            if (end <= start) {
                continue;
            }

            MarkdownTokenType type = classify(start, end, translatableSpans, protectedSpans);
            String value = source.substring(start, end);
            appendMerged(tokens, new MarkdownToken(type, value, start, end));
        }

        return tokens;
    }

    private static void addBoundaries(TreeSet<Integer> boundaries, List<Span> spans) {
        for (Span span : spans) {
            boundaries.add(span.start);
            boundaries.add(span.end);
        }
    }

    private static MarkdownTokenType classify(
            int start, int end, List<Span> translatableSpans, List<Span> protectedSpans) {
        if (isCovered(start, end, protectedSpans)) {
            return MarkdownTokenType.PROTECTED;
        }
        if (isCovered(start, end, translatableSpans)) {
            return MarkdownTokenType.TRANSLATABLE;
        }
        return MarkdownTokenType.STRUCTURAL;
    }

    private static boolean isCovered(int start, int end, List<Span> spans) {
        for (Span span : spans) {
            if (start >= span.start && end <= span.end) {
                return true;
            }
        }
        return false;
    }

    private static void appendMerged(List<MarkdownToken> tokens, MarkdownToken next) {
        if (tokens.isEmpty()) {
            tokens.add(next);
            return;
        }

        MarkdownToken previous = tokens.get(tokens.size() - 1);
        if (previous.getType() == next.getType()
                && previous.getEndOffset() == next.getStartOffset()) {
            MarkdownToken merged =
                    new MarkdownToken(
                            previous.getType(),
                            previous.getTokenId(),
                            previous.getValue() + next.getValue(),
                            previous.getStartOffset(),
                            next.getEndOffset());
            tokens.set(tokens.size() - 1, merged);
            return;
        }

        tokens.add(next);
    }

    private static void assignTranslatableTokenIds(List<MarkdownToken> tokens) {
        int index = 1;
        for (int i = 0; i < tokens.size(); i++) {
            MarkdownToken token = tokens.get(i);
            if (token.getType() == MarkdownTokenType.TRANSLATABLE) {
                tokens.set(i, token.withTokenId("T" + index));
                index++;
            }
        }
    }

    private static final class Span {
        private final int start;
        private final int end;

        private Span(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
