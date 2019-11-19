import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.html2md.converter.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

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
            if (link.getUrl().startsWith("http:")) {
                return link.withUrl("https:" + link.getUrl().substring("http:".length()));
            }
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
            set.add(new HtmlNodeRendererHandler<>("pre", Element.class, this::processPre));
            set.add(new HtmlNodeRendererHandler<>("img", Element.class, this::processImg));
            set.add(new HtmlNodeRendererHandler<>("p", Element.class, this::processP));
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

        private void processPre(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.append("```\n");
            Element code = node.getElementsByTag("code").first();
            out.append(code.attr("lang"));
            out.append(code.ownText());
            out.append("\n```");
            out.blankLine();
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
            if (!StringUtils.isBlank(node.text()) || node.childNodeSize()>0) {
                context.renderChildren(node, false, null);
                out.blankLine();
            }
        }

        private void processBr(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.blankLine();
        }

        private void processH1(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.append("## ");
            out.append(node.text());
            out.blankLine();
        }

        private void processH2(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.append("### ");
            out.append(node.text());
            out.blankLine();
        }

        private void processH3(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.append("#### ");
            out.append(node.text());
            out.blankLine();
        }

        private void processH4(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
            out.append("##### ");
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
                "<pre>\n" +
                " <code class=\"hljs bash copyable\" lang=\"bash\">\n" +
                "    -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps  -XX:+PrintGCTimeStamps\n" +
                "    <span class=\"copy-code-btn\">复制代码</span>\n" +
                " </code>\n" +
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
