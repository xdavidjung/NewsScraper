package edu.washington.cs.knowitall.newsscraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GoogleRssScraper extends RssScraper {

    private final String RSS_PARAM = "&output=rss";
    private final ArticleTextCleaner cleaner;

    // remove the following:
    // private final List<String> publisherNames;
    // private final Map<String, Integer> publisherCount;
    // private final Map<String, List<String>> publisherTexts;

    public GoogleRssScraper(Calendar cal, Config con) {
        super(cal, con);

        cleaner = new ArticleTextCleaner();
        // publisherNames = new ArrayList<String>();
        // publisherCount = new HashMap<String, Integer>();
        // publisherTexts = new HashMap<String, List<String>>();
    }

    public String constructUrl(String category, String feed) {
        return baseUrl + "&ned=" + category + "&topic=" + feed;
    }

    @Override
    public void fetchData() {
        logger.info("fetchData(): Start fetching data.");

        outputLocation = makeDailyDirectory(config.getRootDir());
        rawDataDir = outputLocation + "raw_data/";

        categories = config.getCategories();
        rssCategoryToFeeds = new HashMap<String, List<String>>();
        for (String category: categories) {
            rssCategoryToFeeds.put(category, new ArrayList<String>());
        }

        JsonObject rssList = config.getJsonFeeds();
        for (String category: categories) {
            List<String> feedsToFill = rssCategoryToFeeds.get(category);
            JsonArray feedSource = rssList.get(category).getAsJsonArray();

            for (JsonElement feed: feedSource) {
                feedsToFill.add(feed.getAsString());
            }
        }

        File rawDir = new File(rawDataDir);
        rawDir.mkdirs();

        for (String categoryName: categories) {
            List<String> feeds = rssCategoryToFeeds.get(categoryName);
            for (String feedName: feeds) {
                try {
                    String parentUrl = constructUrl(categoryName, feedName);
                    // parentDoc is the rss feed for the larger topic
                    Document parentDoc = Jsoup.connect(parentUrl).get();

                    // write fetched xml to local data: .../rawdata/DATE_CATEGORY_FEED.html
                    BufferedWriter out =
                        new BufferedWriter(
                        new OutputStreamWriter(
                        new FileOutputStream(
                        new File(rawDataDir + dateString + "_" + categoryName + "_" + feedName + ".html"), true), ENCODE));
                    out.write(parentDoc.toString());

                    for (Element article: parentDoc.getElementsByTag("item")) {
                        String relatedUrl = getRelatedArticleUrl(article);
                        if (relatedUrl == null) continue;

                        Document articleDoc = Jsoup.connect(relatedUrl).get();
                        out.write(articleDoc.toString());
                    }

                    out.close();
                    logger.info(
                            "fetchData(): " + "Fetched {}: {} successfully",
                            categoryName, feedName);

                } catch (IOException e) {
                    logger.error("fetchData(): " + "Failed to download: {}_{}",
                            categoryName, feedName);
                    e.printStackTrace();
                }
            }
        }
        logger.info("fetchData(): End fetching.");
    }


    @Override
    protected void processHtml(String dir, boolean processOnly) {
        logger.info("processHtml(): Start processing HTML.");

        if (!dir.endsWith(FOLDER_PATH_SEPERATOR))
            dir = dir + FOLDER_PATH_SEPERATOR;

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

            // each item contains a news article
            Elements items = wholeHtml.getElementsByTag("item");
            for (Element item : items) {
                // make sure it's today's news
                String pubdate = item.getElementsByTag("pubdate").first().text();
                if (!checkDateMatch(pubdate) && !processOnly) continue;

                // get news' title
                Element titleEle = item.getElementsByTag("title").first();
                String title = titleEle.text().trim();

                // make sure no duplicate news
                if (dataMap.containsKey(title)) continue;

                Element desc = item.getElementsByTag("description").first();
                desc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(desc.toString()));

                NewsData data = new NewsData(categoryName, rssName, title, dateString);

                Element lh = desc.getElementsByClass("lh").first();
                String descText = lh.getElementsByTag("font").get(2).text();
                String publisher = lh.getElementsByTag("font").get(1).text();
                String url = lh.getElementsByTag("a").first().absUrl("href");

                // if (!publisherNames.contains(publisher)) publisherNames.add(publisher);
                // if (!publisherCount.containsKey(publisher)) publisherCount.put(publisher, 0);
                // publisherCount.put(publisher, publisherCount.get(publisher) + 1);
                // if (!publisherTexts.containsKey(publisher)) publisherTexts.put(publisher, new ArrayList<String>());
                // publisherTexts.get(publisher).add(descText);

                data.source = publisher;
                data.url = url;

                descText = cleaner.clean(descText, publisher);
                if (descText == null || descText.equals(""))
                    continue;
                if (descText.length() > sentenceMinimumLength
                        && !duplicateChecker.contains(descText)) {
                    duplicateChecker.add(descText);
                    data.content = descText;
                    dataMap.put(title, data);
                }
            }
        }
        logger.info("processHtml(): End processing HTML.");

        /*
        Collections.sort(publisherNames);
        File folder = new File("publishers/");
        if (!folder.exists())
            folder.mkdir();

        for (String publisher: publisherNames) {
            // print out texts
            try {
                BufferedWriter out =
                    new BufferedWriter(
                    new OutputStreamWriter(
                    new FileOutputStream(
                    new File("publishers/" + publisher.replace(' ', '_').replace('/', '_') + ".txt"), true), ENCODE));

                for (String text: publisherTexts.get(publisher)) {
                    out.write(text + "\n");
                }

                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // print out counts
            try {
                BufferedWriter out =
                    new BufferedWriter(
                    new OutputStreamWriter(
                    new FileOutputStream(
                    new File("publisherCount.txt"), true), ENCODE));

                out.write(publisherCount.get(publisher).toString() + "\t" + publisher + "\n");

                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */
    }

    /* Given an Element from the Google Rss page that has the tag "item",
     * returns the url for related articles for this article.
     *
     * If the url link is "and more" there are no related articles - return null.
     */
    private String getRelatedArticleUrl(Element article) {
        Document artDoc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(article.toString()));

        Element textAndLinks = artDoc.getElementsByClass("lh").first();
        Element lastLink = textAndLinks.getElementsByTag("font").last();
        if (lastLink.text().contains("and more")) return null;
        String url = lastLink.getElementsByTag("a").first().absUrl("href");
        return url + RSS_PARAM;
    }

}
