import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抓取头条分享的文章转化为md文件
 */
public class TouTiaoDeprecated {

    // 生成目录
    static String path = "E:\\我的学习\\study\\docs\\mq";

    // 文章标题
    static String fileName = "Kafka原理";
    // 文章地址
    static String url = "https://www.toutiao.com/a6758731417548489229/?tt_from=weixin&utm_campaign=client_share&wxshare_count=1&timestamp=1573693037&app=news_article&utm_source=weixin&utm_medium=toutiao_android&req_id=2019111408571701001005322202E28F8F&group_id=6758731417548489229";

    // 文章标题
    // static String fileName = "GC原理及调优";
    // 文章地址
    // static String url = "https://www.toutiao.com/a6740803737603801604/?tt_from=weixin&utm_campaign=client_share&wxshare_count=1&timestamp=1573131368&app=news_article&utm_source=weixin&utm_medium=toutiao_android&req_id=201911072056080100140481391D1C3E51&group_id=6740803737603801604";

    static AtomicInteger imgCount = new AtomicInteger(1);

    public static void main(String[] args) throws IOException {
        Document doc = Jsoup.connect(url).get();
        String title = doc.title();
        String regex = "content:(.*?)\\.slice";
        Matcher matcher = Pattern.compile(regex).matcher(doc.body().html());
        while (matcher.find()) {
            String ret = matcher.group(1);
            // 获取HTML
            String html = StringEscapeUtils.unescapeHtml4(unicodeDecode(ret));

            String md = "# " + fileName + "(转)" + "\n\n";
            md += getMarkDownText(html) + "\n";
            md += "**声明**" + "\n\n"
                    + "原文地址: " + "[" + title + "](" + url + ")";
            System.out.println(md);
            saveStrAsFile(path, fileName + ".md", md);
        }
    }

    public static String unicodeDecode(String string) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(string);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            string = string.replace(matcher.group(1), ch + "");
        }
        return string;
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

    public static String getMarkDownText(String html) {
        StringBuilder result = new StringBuilder();

        Document document = Jsoup.parseBodyFragment(html.replace("&nbsp;", " "));

        Elements allElements = document.body().children();

        if (allElements.size() == 1) {
            for (Node node : allElements.get(0).childNodes()) {
                result.append(handleNode(node));
            }
        } else {
            for (Node node : document.body().childNodes()) {
                result.append(handleNode(node));
            }
        }

        // 遍历所有直接子节点
        // Element element = document.getElementsByTag("div").first();
        // Element element = document.body();

        // for (Node node : element.childNodes()) {
        //     result.append(handleNode(node));
        // }
        return result.toString();
    }

    /**
     * 处理Node，目前支持处理p、pre、ul和ol四种节点
     *
     * @param node
     * @return
     */
    private static String handleNode(Node node) {
        String nodeName = node.nodeName();
        String nodeStr = node.toString();
        switch (nodeName) {
            case "h1":
                Element h1Element = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("h1").first();
                String h1Str = h1Element.text();
                return "## " + h1Str + "\n\n";
            case "h2":
                Element h2Element = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("h2").first();
                String h2Str = h2Element.text();
                return "### " + h2Str + "\n\n";
            case "h3":
                Element h3Element = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("h3").first();
                String h3Str = h3Element.text();
                return "#### " + h3Str + "\n\n";
            case "p":
                Element pElement = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("p").first();
                String pStr = pElement.html();
                for (Element child : pElement.children()) {
                    pStr = handleInnerHtml(pStr, child);
                }
                return pStr + "\n\n";
            case "pre":
                return "```\n" + Jsoup.parseBodyFragment(nodeStr.replaceAll("<br>", "\n")).body().text() + "\n```\n";
            case "ul":
                Element ulElement = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("ul").first();
                String ulStr = ulElement.html().replace("<li>", "- ").replace("</li>", "");
                for (Element li : ulElement.getElementsByTag("li")) {
                    for (Element child : li.children()) {
                        ulStr = handleInnerHtml(ulStr, child);
                    }
                }
                return ulStr + "\n\n";
            case "ol":
                Element olElement = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("ol").first();
                String olStr = olElement.html();

                Elements liElements = olElement.getElementsByTag("li");
                for (int i = 1; i <= liElements.size(); i++) {
                    Element li = liElements.get(i - 1);
                    olStr = olStr.replace(li.toString(), li.toString().replace("<li>", i + ". ").replace("</li>", ""));

                    for (Element child : li.children()) {
                        olStr = handleInnerHtml(olStr, child);
                    }
                }
                return olStr + "\n";
            // 图片
            case "div":
                Element imgElement = Jsoup.parseBodyFragment(nodeStr).body().getElementsByTag("img").first();
                String imgStr = imgElement.attr("src").replaceAll("\\\\\"", "");

                int imgName = imgCount.getAndAdd(1);
                downImages(path + "/" + fileName, imgName + ".png", imgStr);

                return "![" + fileName + "_图" + imgName + "](./" + fileName + "/" + imgName + ".png" + ") " + "\n ";
            // 非HTML元素
            case "#text":
                return "\n";
        }
        return "";
    }

    /**
     * 处理innerHTML中的HTML元素，目前支持处理的子元素包括strong、img、em
     *
     * @param innerHTML
     * @param child
     * @return
     */
    private static String handleInnerHtml(String innerHTML, Element child) {
        switch (child.tag().toString()) {
            case "strong":
                innerHTML = innerHTML.replace(child.toString(), "**" + child.text() + "**");
                break;
            case "img":
                String src = child.attr("src");
                if (src.charAt(0) == '/') {
                    src = "https://leetcode-cn.com" + src;
                }

                innerHTML = innerHTML.replace(child.toString(), "![img](" + src + ")");
                break;
            case "em":
                innerHTML = innerHTML.replace(child.toString(), " *" + child.text() + "* ");
                break;
            default:
                innerHTML = innerHTML.replace(child.toString(), child.text());
                break;
        }
        return innerHTML;
    }

    /**
     * 根据图片的外网地址下载图片到本地硬盘的filePath
     *
     * @param filePath 本地保存图片的文件路径
     * @param imgUrl   图片的外网地址
     * @throws UnsupportedEncodingException
     */
    public static void downImages(String filePath, String name, String imgUrl) {
        try {
            //图片url中的前面部分：例如"http://images.csdn.net/"
            String beforeUrl = imgUrl.substring(0, imgUrl.lastIndexOf("/") + 1);
            //图片url中的后面部分：例如“20150529/PP6A7429_副本1.jpg”
            String fileName = imgUrl.substring(imgUrl.lastIndexOf("/") + 1);
            //编码之后的fileName，空格会变成字符"+"
            String newFileName = URLEncoder.encode(fileName, "UTF-8");
            //把编码之后的fileName中的字符"+"，替换为UTF-8中的空格表示："%20"
            newFileName = newFileName.replaceAll("\\+", "\\%20");
            //编码之后的url
            imgUrl = beforeUrl + newFileName;

            //创建文件目录
            File files = new File(filePath);
            if (!files.exists()) {
                files.mkdirs();
            }
            //获取下载地址
            URL url = new URL(imgUrl);
            //链接网络地址
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            //获取链接的输出流
            InputStream is = connection.getInputStream();
            //创建文件，fileName为编码之前的文件名
            File file = new File(filePath, name);
            //根据输入流写入文件
            FileOutputStream out = new FileOutputStream(file);
            int i = 0;
            while ((i = is.read()) != -1) {
                out.write(i);
            }
            out.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
