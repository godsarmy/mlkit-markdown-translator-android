package io.github.godsarmy.mlmarkdown.markdown;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Step 6A prototype for parsing Markdown and inspecting AST node categories.
 */
public final class FlexmarkAstInspector {
    private final Parser parser;

    public FlexmarkAstInspector() {
        this.parser = Parser.builder().build();
    }

    public Set<String> inspectNodeTypes(String markdown) {
        Node document = parser.parse(markdown);
        Set<String> categories = new LinkedHashSet<>();
        collectNodeCategories(document, categories);
        return categories;
    }

    private static void collectNodeCategories(Node node, Set<String> categories) {
        categories.add(mapCategory(node));
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            collectNodeCategories(child, categories);
        }
    }

    private static String mapCategory(Node node) {
        if (node instanceof Paragraph) {
            return "paragraph";
        }
        if (node instanceof Heading) {
            return "heading";
        }
        if (node instanceof Text) {
            return "text";
        }
        if (node instanceof Emphasis) {
            return "emphasis";
        }
        if (node instanceof StrongEmphasis) {
            return "strong_emphasis";
        }
        if (node instanceof BulletList) {
            return "bullet_list";
        }
        if (node instanceof OrderedList) {
            return "ordered_list";
        }
        if (node instanceof ListItem) {
            return "list_item";
        }
        if (node instanceof BlockQuote) {
            return "blockquote";
        }
        if (node instanceof FencedCodeBlock) {
            return "fenced_code_block";
        }
        if (node instanceof Code) {
            return "code_span";
        }
        if (node instanceof Link) {
            return "link";
        }
        if (node instanceof Image) {
            return "image";
        }
        return "other:" + node.getClass().getSimpleName();
    }
}
