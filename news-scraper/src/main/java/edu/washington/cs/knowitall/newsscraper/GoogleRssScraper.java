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

public class GoogleRssScraper extends RssScraper {

    public GoogleRssScraper(Calendar cal, Config con) {
        super(cal, con);
    }

    public String constructUrl(String category, String feed) {
        return baseUrl + "&ned=" + category + "&topic=" + feed;
    }

    @Override
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

            // each item contains a news article
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
                            // getURL(item, data);

                            // getSource(item, data);

                            // getImageUrl(item, data);

                            Element lh = desc.getElementsByClass("lh").first();
                            String text = lh.getElementsByTag("font").get(2).text();
                            System.out.println(title + ": " + text);

                            // description has no child tag
                            if (para == null) {

                                // TODO: this doesn't work, as this grabs the names of the extra links as well.
                                String descText = desc.text().trim();
                                // descText = fixContent(descText);
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
                                    // paraText = fixContent(paraText);
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

}
