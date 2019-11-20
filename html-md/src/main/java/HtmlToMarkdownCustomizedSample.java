import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.html2md.converter.*;
import com.vladsch.flexmark.html2md.converter.internal.HtmlConverterCoreNodeRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.Utils;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.html.LineFormattingAppendable;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.BasedSequenceImpl;
import com.vladsch.flexmark.util.sequence.RepeatedCharSequence;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HtmlToMarkdownCustomizedSample {
    static class CustomLinkResolver implements HtmlLinkResolver {
        public CustomLinkResolver(HtmlNodeConverterContext context) {
        }

        @Override
        public ResolvedLink resolveLink(Node node, HtmlNodeConverterContext context, ResolvedLink link) {
            // convert all links from http:// to https://
            // if (link.getUrl().startsWith("http:")) {
            //     return link.withUrl("https:" + link.getUrl().substring("http:".length()));
            // }
            return link;
        }

        static class Factory implements HtmlLinkResolverFactory {
            @Override
            public Set<Class<? extends HtmlLinkResolverFactory>> getAfterDependents() {
                return null;
            }

            @Override
            public Set<Class<? extends HtmlLinkResolverFactory>> getBeforeDependents() {
                return null;
            }

            @Override
            public boolean affectsGlobalScope() {
                return false;
            }

            @Override
            public HtmlLinkResolver apply(HtmlNodeConverterContext context) {
                return new CustomLinkResolver(context);
            }
        }
    }

    static class HtmlConverterTextExtension implements FlexmarkHtmlConverter.HtmlConverterExtension {
        public static HtmlConverterTextExtension create() {
            return new HtmlConverterTextExtension();
        }

        @Override
        public void rendererOptions(MutableDataHolder options) {

        }

        @Override
        public void extend(FlexmarkHtmlConverter.Builder builder) {
            builder.linkResolverFactory(new CustomLinkResolver.Factory());
            builder.htmlNodeRendererFactory(new CustomHtmlNodeConverter.Factory());
        }
    }

    static class CustomHtmlNodeConverter implements HtmlNodeRenderer {
        AtomicInteger imgCount = new AtomicInteger(1);

        public CustomHtmlNodeConverter(DataHolder options) {

        }

        @Override
        public Set<HtmlNodeRendererHandler<?>> getHtmlNodeRendererHandlers() {
            Set<HtmlNodeRendererHandler<?>> set = new HashSet<>();
            set.add(new HtmlNodeRendererHandler<>("kbd", Element.class, this::processKbd));
            set.add(new HtmlNodeRendererHandler<>("blockquote", Element.class, this::processBlockQuote));
            set.add(new HtmlNodeRendererHandler<>("pre", Element.class, this::processPre));
            set.add(new HtmlNodeRendererHandler<>("img", Element.class, this::processImg));
            // set.add(new HtmlNodeRendererHandler<>("p", Element.class, this::processP));
            set.add(new HtmlNodeRendererHandler<>("br", Element.class, this::processBr));
            set.add(new HtmlNodeRendererHandler<>("h1", Element.class, this::processH1));
            set.add(new HtmlNodeRendererHandler<>("h2", Element.class, this::processH2));
            set.add(new HtmlNodeRendererHandler<>("h3", Element.class, this::processH3));
            set.add(new HtmlNodeRendererHandler<>("h4", Element.class, this::processH4));
            return set;
        }

        private void processKbd(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.append("喝哈");
            context.renderChildren(node, false, null);
            out.append("喝哈");
        }

        private void processBlockQuote(Element element, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            if (HtmlConverterCoreNodeRenderer.isFirstChild(element)) {
                out.line();
            }
            out.pushPrefix();
            out.addPrefix("> ");
            context.renderChildren(element, true, null);
            out.line();
            out.popPrefix();
        }

        /**
         * 参考 HtmlConverterCoreNodeRenderer#processPre
         *
         * @param node
         * @param context
         * @param out
         */
        private void processPre(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            // out.append("```\n");
            // Element code = node.getElementsByTag("code").first();
            // out.append(code.attr("lang"));
            // out.append(code.ownText());
            // out.append("\n```");
            // out.blankLine();


            context.pushState(node);

            String text;
            boolean hadCode = false;
            String className = "";

            HtmlNodeConverterContext preText = context.getSubContext();
            preText.getMarkdown().setOptions(out.getOptions() & ~(LineFormattingAppendable.COLLAPSE_WHITESPACE | LineFormattingAppendable.SUPPRESS_TRAILING_WHITESPACE));
            preText.getMarkdown().openPreFormatted(false);

            Node next;
            while ((next = context.next()) != null) {
                if (next.nodeName().equalsIgnoreCase("code") || next.nodeName().equalsIgnoreCase("tt")) {
                    hadCode = true;
                    Element code = (Element) next;
                    //text = code.toString();
                    preText.renderChildren(code, false, null);
                    // class="language-java hljs" -> java
                    if (className.isEmpty())
                        className = Utils.removeStart(code.className().replaceAll("hljs", "").replaceAll("copyable", ""), "language-");
                } else if (next.nodeName().equalsIgnoreCase("br")) {
                    preText.getMarkdown().append("\n");
                } else if (next.nodeName().equalsIgnoreCase("#text")) {
                    preText.getMarkdown().append(((TextNode) next).getWholeText());
                } else {
                    preText.renderChildren(next, false, null);
                }
            }

            preText.getMarkdown().closePreFormatted();
            text = preText.getMarkdown().toString(2);

            //int start = text.indexOf('>');
            //int end = text.lastIndexOf('<');
            //text = text.substring(start + 1, end);
            //text = Escaping.unescapeHtml(text);

            int backTickCount = HtmlConverterCoreNodeRenderer.getMaxRepeatedChars(text, '`', 3);
            CharSequence backTicks = RepeatedCharSequence.of("`", backTickCount);

            if (!false && (!className.isEmpty() || text.trim().isEmpty() || !hadCode)) {
                out.blankLine().append(backTicks);
                if (!className.isEmpty()) {
                    out.append(className).append("\t");
                }
                out.line();
                out.openPreFormatted(true);
                out.append(text.isEmpty() ? "\n" : text.replaceAll("复制代码\n", ""));
                out.closePreFormatted();
                out.line().append(backTicks).line();
                out.tailBlankLine();
            } else {
                // we indent the whole thing by 4 spaces
                out.blankLine();
                out.pushPrefix();
                out.addPrefix("    ");
                out.openPreFormatted(true);
                out.append(text.isEmpty() ? "\n" : text);
                out.closePreFormatted();
                out.line();
                out.tailBlankLine();
                out.popPrefix();
            }

            context.popState(out);
        }

        private void processImg(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            // String alt = node.attr("alt");
            String src = node.attr("src");
            if (StringUtils.isBlank(src)) {
                src = node.attr("data-src");
            }
            out.append(String.format("![图-%s](%s)", imgCount.getAndIncrement(), src));
            out.blankLine();
        }

        private void processP(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            // boolean isItemParagraph = false;
            // boolean isDefinitionItemParagraph = false;
            //
            // Element firstElementSibling = node.firstElementSibling();
            // if (firstElementSibling == null || node == firstElementSibling) {
            //     String tagName = node.parent().tagName();
            //     isItemParagraph = tagName.equalsIgnoreCase("li");
            //     isDefinitionItemParagraph = tagName.equalsIgnoreCase("dd");
            // }
            //
            // out.blankLineIf(!(isItemParagraph || isDefinitionItemParagraph || isFirstChild(element)));
            //
            // if (node.childNodeSize() == 0) {
            //     if (myHtmlConverterOptions.brAsExtraBlankLines) {
            //         out.append("<br />").blankLine();
            //     }
            // } else {
            //     context.processTextNodes(node, false);
            // }
            //
            // out.line();
            //
            // if (isItemParagraph || isDefinitionItemParagraph) {
            //     out.tailBlankLine();
            // }
        }

        private void processBr(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.blankLine();
        }

        private void processH1(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.blankLine();
            out.append("## ");
            out.append(node.text());
            out.blankLine();
        }

        private void processH2(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.blankLine();
            out.append("### ");
            out.append(node.text());
            out.blankLine();
        }

        private void processH3(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.blankLine();
            out.append("#### ");
            out.append(node.text());
            out.blankLine();
        }

        private void processH4(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.blankLine();
            out.append("#### ");
            out.append(node.text());
            out.blankLine();
        }

        static class Factory implements HtmlNodeRendererFactory {
            @Override
            public HtmlNodeRenderer apply(DataHolder options) {
                return new CustomHtmlNodeConverter(options);
            }
        }
    }

    public static void main(String[] args) {
        MutableDataSet options = new MutableDataSet()
                .set(FlexmarkHtmlConverter.BR_AS_EXTRA_BLANK_LINES, false)
                .set(Parser.EXTENSIONS, Collections.singletonList(HtmlConverterTextExtension.create()));

        String html = "" +
                "<h1>呵呵</h1>" +
                "<p>哈哈</p>" +
                "<p></p>" +
                "<figure>\n" +
                " <img alt=\"分代收集算法\" class=\"lazyload\" data-src=\"https://user-gold-cdn.xitu.io/2019/9/25/16d68b1db4a3a514?imageView2/0/w/1280/h/960/ignore-error/1\" data-width=\"756\" data-height=\"329\">\n" +
                " <figcaption></figcaption>\n" +
                "</figure>" +
                "<br/>" +
                "<pre>" +
                " <code class=\"hljs bash copyable\" lang=\"bash\">" +
                "    -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps  -XX:+PrintGCTimeStamps" +
                "    <span class=\"copy-code-btn\">复制代码</span>" +
                " </code>" +
                "</pre>" +
                "<ul>\n" +
                "  <li>\n" +
                "    <p>Add: live templates starting with <code>.</code> <kbd>Kbd</kbd> <a href='http://example.com'>link</a></p>\n" +
                "    <table>\n" +
                "      <thead>\n" +
                "        <tr><th> Element       </th><th> Abbreviation    </th><th> Expansion                                               </th></tr>\n" +
                "      </thead>\n" +
                "      <tbody>\n" +
                "        <tr><td> Abbreviation  </td><td> <code>.abbreviation</code> </td><td> <code>*[]:</code>                                                 </td></tr>\n" +
                "        <tr><td> Code fence    </td><td> <code>.codefence</code>    </td><td> ``` ... ```                                       </td></tr>\n" +
                "        <tr><td> Explicit link </td><td> <code>.link</code>         </td><td> <code>[]()</code>                                                  </td></tr>\n" +
                "      </tbody>\n" +
                "    </table>\n" +
                "  </li>\n" +
                "</ul>";
        String markdown = FlexmarkHtmlConverter.builder(options).build().convert(html);

        System.out.println("HTML:");
        System.out.println(html);

        System.out.println("Markdown:");
        System.out.println(markdown);

        String regex = "!\\[(.*)\\]\\((.*)\\)";
        Matcher matcher = Pattern.compile(regex).matcher(markdown);
        while (matcher.find()) {
            String imgSrc = matcher.group(2);
            System.out.println(imgSrc);
        }

        saveStrAsFile("E:\\我的学习\\study\\docs\\others", "test.md", markdown);

        System.out.println("hello\tworld");
    }

    /**
     * 保存文件
     *
     * @param path     路径
     * @param fileName 文件名
     * @param content  内容
     */
    public static void saveStrAsFile(String path, String fileName, String content) {
        FileWriter fwriter = null;
        try {
            fwriter = new FileWriter(new File(path, fileName));
            fwriter.write(content);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                fwriter.flush();
                fwriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
