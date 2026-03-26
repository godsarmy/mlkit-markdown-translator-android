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
        if (node instanceof Paragraph || hasSimpleName(node, "Paragraph")) {
            return "paragraph";
        }
        if (node instanceof Heading || hasSimpleName(node, "Heading")) {
            return "heading";
        }
        if (node instanceof Text || hasSimpleName(node, "Text")) {
            return "text";
        }
        if (node instanceof Emphasis || hasSimpleName(node, "Emphasis")) {
            return "emphasis";
        }
        if (node instanceof StrongEmphasis || hasSimpleName(node, "StrongEmphasis")) {
            return "strong_emphasis";
        }
        if (node instanceof BulletList || hasSimpleName(node, "BulletList")) {
            return "bullet_list";
        }
        if (node instanceof OrderedList || hasSimpleName(node, "OrderedList")) {
            return "ordered_list";
        }
        if (node instanceof ListItem || hasSimpleName(node, "ListItem")) {
            return "list_item";
        }
        if (node instanceof BlockQuote || hasSimpleName(node, "BlockQuote")) {
            return "blockquote";
        }
        if (node instanceof FencedCodeBlock || hasSimpleName(node, "FencedCodeBlock")) {
            return "fenced_code_block";
        }
        if (node instanceof Code || hasSimpleName(node, "Code")) {
            return "code_span";
        }
        if (node instanceof Link || hasSimpleName(node, "Link")) {
            return "link";
        }
        if (node instanceof Image || hasSimpleName(node, "Image")) {
            return "image";
        }
        return "other:" + node.getClass().getSimpleName();
    }

    private static boolean hasSimpleName(Node node, String simpleName) {
        return simpleName.equals(node.getClass().getSimpleName());
    }
}
