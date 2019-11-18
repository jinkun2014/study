import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抓取头条分享的文章转化为md文件
 */
public class TouTiao {
    // 生成目录
    static String path = "E:\\我的学习\\study\\docs\\mq";

    // 文章标题
    static String fileName = "Kafka原理";
    // 文章地址
    static String url = "https://www.toutiao.com/a6758731417548489229/?tt_from=weixin&utm_campaign=client_share&wxshare_count=1&timestamp=1573693037&app=news_article&utm_source=weixin&utm_medium=toutiao_android&req_id=2019111408571701001005322202E28F8F&group_id=6758731417548489229";

    static AtomicInteger imgCount = new AtomicInteger(1);

    public static void main(String[] args) throws IOException {
        // 下载文件
        Document doc = Jsoup.connect(url).get();
        String title = doc.title();

        // Html内容
        String html = "";
        String regex = "content: \'(.*?)\'\\.slice";
        Matcher matcher = Pattern.compile(regex).matcher(doc.body().html());
        while (matcher.find()) {
            String ret = matcher.group(1);
            // 获取HTML
            html = StringEscapeUtils.unescapeHtml4(unicodeDecode(ret)).replaceAll("\\\\\"", "").replaceAll("\"", "");
        }
        System.out.println("html:" + html);

        // 解析Markdown
        MutableDataSet options = new MutableDataSet()
                .set(Parser.EXTENSIONS, Collections.singletonList(HtmlToMarkdownCustomizedSample.HtmlConverterTextExtension.create()));
        String markdown = FlexmarkHtmlConverter.builder(options).build().convert(html);
        System.out.println("markdown:" + markdown);

        // 下载图片
        Map<String, String> imgMap = new HashMap<>();
        regex = "!\\[(.*)\\]\\((.*)\\)";
        matcher = Pattern.compile(regex).matcher(markdown);
        while (matcher.find()) {
            String imgSrc = matcher.group(2);
            System.out.println(imgSrc);
            String imageName = imgCount.getAndAdd(1) + ".png";
            downImages(path + "/" + fileName, imageName, imgSrc);
            imgMap.put(imgSrc, "./" + fileName + "/" + imageName);
        }
        // 替换图片
        for (String key : imgMap.keySet()) {
            markdown = markdown.replace(key, imgMap.get(key));
        }

        markdown = markdown
                + "## 声明" + "\n\n"
                + "原文地址: " + "[" + title + "](" + url + ")";

        // 保存md文件
        saveStrAsFile(path, fileName + ".md", markdown);
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

    /**
     * 根据图片的外网地址下载图片到本地硬盘的filePath
     *
     * @param filePath 本地保存图片的文件路径
     * @param imgUrl   图片的外网地址
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
