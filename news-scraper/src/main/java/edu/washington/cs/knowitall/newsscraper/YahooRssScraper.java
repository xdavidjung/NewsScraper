package edu.washington.cs.knowitall.newsscraper;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * This class fetches the RSS data from Yahoo and stores the metadata to the
 * database.
 *
 * @author Pingyang He, David H Jung
 *
 */
public class YahooRssScraper extends RssScraper {

    private final String TAG_LINK = "<link />";
    private final String TAG_SOURCE = "</source>";
    private final String REUTERS_KEYWORD = "(Reuters) - ";
    private final String HEALTHYDAY_KEYWORD = "(HealthDay News) -- ";
    private final String LINK_GARBAGE_TAIL = "\n";
    private final String GARBAGE_TAIL = "...";
    private final String USELESS_CONTENT_INDICATOR = "[...]";
    private final String[] ENDING_PUNCTUATION = { ".", "?", "!", ".\"", "?\"", "!\"" };

    /**
     * @param cal
     *            Today's date
     * @param con
     *            The configuration file object.
     */
    public YahooRssScraper(Calendar cal, Config con) {
        super(cal, con);
    }

    public String constructUrl(String category, String feed) {
        return baseUrl + feed;
    }

    /*
     * Parse the files in the given directory into the map.
     * @param dir the directory containing the raw data files.
     * @param processOnly true if we have not fetched raw data.
     */
    protected void processHtml(String dir, boolean processOnly) {

        if (!dir.endsWith(FOLDER_PATH_SEPERATOR))
            dir = dir + FOLDER_PATH_SEPERATOR;

        logger.info("processHtml(): Start processing HTML.");

        duplicateChecker = new HashSet<String>();
        dataMap = new HashMap<String, NewsData>();

        // files is a grab of all the files in the given dir.
        File rawDataFile = new File(dir);
        String[] files = rawDataFile.list();

        // grab the date string for this folder.
        if (processOnly && files.length > 0) {
            String fileDate = getFileDate(files[0]);
            if (fileDate != null)
                dateString = fileDate;
        }

        for (String fileName : files) {
            logger.info("processHtml(): Process {}", fileName);
            int timeSeperatorPos = fileName.indexOf('_');
            int catSeperatorPos = fileName.indexOf('_', timeSeperatorPos + 1);

            String categoryName = fileName.substring(timeSeperatorPos + 1,
                                                     catSeperatorPos);
            String rssName = fileName.substring(catSeperatorPos + 1,
                                                fileName.indexOf('.'));

            // read rss file from local disk
            String fileContent = getFileContent(dir + fileName, ENCODE);

            Document wholeHtml = Jsoup.parse(fileContent);

            // each item contains a news
            Elements items = wholeHtml.getElementsByTag("item");
            for (Element item : items) {
                try {
                    // make sure it's today's news
                    String pubdate = item.getElementsByTag("pubdate").first().text();
                    if (checkDateMatch(pubdate) || processOnly) {

                        // get news' title
                        Element titleEle = item.getElementsByTag("title").first();
                        String title = titleEle.text().trim();

                        // make sure no duplicate news
                        if (!dataMap.containsKey(title)) {

                            Element desc = item.getElementsByTag("description").first();
                            desc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(desc.toString()));

                            Element para = desc.getElementsByTag("p").first();
                            NewsData data = new NewsData(categoryName, rssName,
                                    title, dateString);
                            getURL(item, data);

                            getSource(item, data);

                            getImageUrl(item, data);

                            // description has no child tag
                            if (para == null) {

                                // length check
                                String descText = desc.text().trim();
                                descText = fixContent(descText);
                                if (descText == null)
                                    continue;
                                if (descText.length() > sentenceMinimumLength
                                        && !duplicateChecker.contains(descText)) {
                                    duplicateChecker.add(descText);
                                    data.content = descText;
                                    dataMap.put(title, data);
                                }
                            } else {

                                // length check
                                String paraText = para.text().trim();
                                if (paraText.length() > sentenceMinimumLength) {
                                    paraText = fixContent(paraText);
                                    if (paraText == null)
                                        continue;
                                    if (duplicateChecker.contains(paraText))
                                        continue;
                                    duplicateChecker.add(paraText);
                                    data.content = paraText;
                                }

                                try {
                                    // process image info
                                    Element img = para.getElementsByTag("a")
                                            .first().getElementsByTag("img")
                                            .first();
                                    if (data.imgUrl.length() < 1)
                                        data.imgUrl = img.attr("src");
                                    String imgAlt = img.attr("alt").trim();
                                    if (imgAlt.length() > sentenceMinimumLength
                                            && !duplicateChecker.contains(imgAlt)) {
                                        data.imgAlt = imgAlt;
                                        duplicateChecker.add(imgAlt);
                                    }

                                    String imgTitle = img.attr("title");
                                    if (imgTitle.length() > sentenceMinimumLength
                                            && !duplicateChecker.contains(imgTitle)) {
                                        data.imgTitle = img.attr("title");
                                        duplicateChecker.add(imgTitle);
                                    }
                                } catch (NullPointerException e) {
                                    String[] params = { categoryName, rssName,
                                            title };
                                    logger.error("processHtml(): "
                                            + "{}: {}: {} -- has no image.",
                                            params);
                                }
                                dataMap.put(title, data);
                                // simpleDataMap.add(sData);
                            }
                        }
                    }
                } catch (Exception e) {
                    Object[] params = { this, categoryName, rssName,
                            e.getMessage() };
                    logger.error("YahooRssScraper: processHtml(): "
                            + "{}: {} {}", params);
                    e.printStackTrace();
                }
            }
        }
        logger.info("processHtml(): End processing HTML.");
    }

    private void getImageUrl(Element item, NewsData data) {
        Elements allElements = item.getAllElements();
        for (Element element : allElements) {
            if (element.tagName().equals("media:text")) {
                Element content = Jsoup.parse(StringEscapeUtils
                        .unescapeHtml4(element.toString()));
                Elements allContent = content.getAllElements();
                for (Element c : allContent) {
                    if (c.tagName().equals("img")) {
                        String imageUrl = element.attr("src");
                        if (imageUrl != null && imageUrl.length() > 0) {
                            logger.info("getImageUrl(): {}.", imageUrl);
                            data.imgUrl = imageUrl;
                            break;
                        }
                    }
                }
            }
        }
    }

    /*
     * Gets the source of a given site and then stores it in data.
     */
    private void getSource(Element item, NewsData data) {
        try {
            String source = item.getElementsByTag("source").first().text();

            if (source.length() < 1) {
                String itemText = item.html();
                int sourceTagPos = itemText.indexOf(TAG_SOURCE);
                int sourceEndPos = itemText.indexOf('<', sourceTagPos + 1);
                if (sourceTagPos >= 0 && sourceEndPos >= 0)
                    source = removeNewLineTail(itemText.substring(sourceTagPos
                            + TAG_SOURCE.length(), sourceEndPos));
            }

            data.source = source;
        } catch (Exception e) {
            return;
        }
    }

    /*
     * Gets the URL of the given item and stores it in data.
     */
    private void getURL(Element item, NewsData data) {
        try {
            String url = item.getElementsByTag("link").first().text().trim();
            if (url.length() < 1) {
                String itemText = item.html();
                int linkTagPos = itemText.indexOf(TAG_LINK);
                int linkEndPos = itemText.indexOf('<', linkTagPos + 1);
                if (linkTagPos >= 0 && linkEndPos >= 0)
                    url = removeNewLineTail(itemText.substring(linkTagPos
                            + TAG_LINK.length(), linkEndPos));
            }
            data.url = url.trim();
        } catch (Exception e) {
            return;
        }
    }

    /*
     * Removes a new line character from the end of a String.
     */
    private String removeNewLineTail(String str) {
        if (str.endsWith(LINK_GARBAGE_TAIL))
            return str.substring(0, str.length() - LINK_GARBAGE_TAIL.length())
                    .trim();
        return str;
    }

    /*
     * Gets rid of useless information in a paragraph. Text is useless if it
     * ends in the USELESS_CONTENT_INDICATOR.
     *
     * @return the argument String sans useless information; if the whole
     * argument String is useless, returns null.
     */
    private String fixContent(String paraText) {

        if (paraText.endsWith(USELESS_CONTENT_INDICATOR))
            return null;

        // get rid of the leading publisher info
        int pubSep = paraText.indexOf(REUTERS_KEYWORD);
        if (pubSep >= 0)
            paraText = paraText.substring(pubSep + REUTERS_KEYWORD.length());

        int HealthyDayPos = paraText.indexOf(HEALTHYDAY_KEYWORD);
        if (HealthyDayPos >= 0)
            paraText = paraText.substring(HealthyDayPos
                    + HEALTHYDAY_KEYWORD.length());

        if (paraText.endsWith(GARBAGE_TAIL)) {
            // get rid of the "..." at the end of the content
            paraText = paraText.substring(0, paraText.length() - 3).trim();
            for (int i = 0; i < ENDING_PUNCTUATION.length; i++) {
                if (paraText.endsWith(ENDING_PUNCTUATION[i]))
                    return paraText;
            }
        } else
            return paraText;

        return null;
    }
}
