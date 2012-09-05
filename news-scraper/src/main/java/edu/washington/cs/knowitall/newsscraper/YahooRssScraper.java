package edu.washington.cs.knowitall.newsscraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

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

    private final String OUTPUT_DATABASE_NAME = "yahoo_rss.data";
    private final String TAG_LINK = "<link />";
    private final String TAG_SOURCE = "</source>";
    private final String REUTERS_KEYWORD = "(Reuters) - ";
    private final String HEALTHYDAY_KEYWORD = "(HealthDay News) -- ";
    private final String LINK_GARBAGE_TAIL = "\n";
    private final String GARBAGE_TAIL = "...";
    private final String USELESS_CONTENT_INDICATOR = "[...]";
    private final String[] ENDING_PUNCTUATION = { ".", "?", "!", ".\"", "?\"", "!\"" };

    private Map<String, NewsData> dataMap;
    private Set<String> duplicateChecker;

    /**
     * @param cal
     *            Today's date
     * @param con
     *            The configuration file object.
     */
    public YahooRssScraper(Calendar cal, Config con) {
        super(cal, con);
    }

    public void processData(String sourceDir, String targetDir) {

        if (sourceDir == null && targetDir == null) {
            processHtml(rawDataDir, false);

        } else if (sourceDir != null && targetDir != null) {
            outputLocation = targetDir.trim();
            if (!outputLocation.endsWith(FOLDER_PATH_SEPERATOR))
                outputLocation += FOLDER_PATH_SEPERATOR;

            File outputFile = new File(outputLocation);
            outputFile.mkdirs();

            if (!outputFile.exists())
                logger.error("scrape(): "
                        + "Failure to create target folder.");

            processHtml(sourceDir, true);
        }

        outputDatabase();
    }

    /*
     * Parse the files in the given directory into the map.
     * @param dir the directory containing the raw data files.
     * @param processOnly true if we have not fetched raw data.
     */
    private void processHtml(String dir, boolean processOnly) {

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
                    String pubdate = item.getElementsByTag("pubdate").first()
                            .text();
                    if (checkDateMatch(pubdate) || processOnly) {

                        // get news' title
                        Element titleEle = item.getElementsByTag("title")
                                .first();
                        String title = titleEle.text().trim();

                        // make sure no duplicate news
                        if (!dataMap.containsKey(title)) {

                            // make sure it's today's news
                            Element desc = item.getElementsByTag("description")
                                    .first();
                            desc = Jsoup.parse(StringEscapeUtils
                                    .unescapeHtml4(desc.toString()));

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

    /*
     * Outputs the the map data to the database in JSON format.
     */
    private void outputDatabase() {
        logger.info("outputDatabase(): Outputting news data.");

        // load id number from last time
        long prevCount;
        long currentCount;
        File idCountFile = new File(ID_COUNT_FILE_NAME);
        try {
            Scanner sc = new Scanner(idCountFile);
            sc.nextInt();
            prevCount = sc.nextLong();
            currentCount = prevCount + 1;
        } catch (FileNotFoundException e) {
            logger.error("outputDatabase(): Can't find idCount file.");
            currentCount = -1;
            prevCount = -1;
        }

        try {

            String dataLocation = outputLocation + "data/";
            File f = new File(dataLocation);
            f.mkdirs();

            String rssData = dataLocation + dateString + "_"
                    + OUTPUT_DATABASE_NAME;
            File dataFile = new File(rssData);
            dataFile.createNewFile();

            // not using JSON since converting json to string doesn't support
            // unicode
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            String seperator = ", ";
            for (String title : dataMap.keySet()) {
                sb.append("\"" + currentCount++ + "\": "
                        + dataMap.get(title).toJsonString() + seperator);
            }
            // fence post problem
            if (!dataMap.isEmpty())
                sb.delete(sb.length() - seperator.length(), sb.length());

            sb.append("}");
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(new File(rssData)), ENCODE));
            if (sb.length() < 100)
                logger.error("outputDatabase(): " + "Output data is too short.");
            out.write(sb.toString());
            out.close();

        } catch (Exception e) {
            logger.error("outputDatabase(): " + "Failure to output data.");
            e.printStackTrace();
        }

        // write the new id count to file
        FileWriter idCountStream;
        try {
            idCountStream = new FileWriter(ID_COUNT_FILE_NAME);
            BufferedWriter idOut = new BufferedWriter(idCountStream);
            idOut.write(prevCount + " " + currentCount);
            idOut.close();
        } catch (IOException e) {
            logger.error("outputDatabase(): " + "Failure to increase id count.");
            e.printStackTrace();
        }
        logger.info("outputDatabase(): Finished outputting news data.");

    }

    /*
     * Check if the given date String is the same as dateString
     */
    private boolean checkDateMatch(String pubdate) {
        if (pubdate.substring(0, 10).equals(dateString)) {
            return true;
        }
        try {
            Date d = dateFormat.parse(dateString);
            int dayPos = pubdate.indexOf(' ');
            int monthPos = pubdate.indexOf(' ', dayPos + 1);
            int yearPos = pubdate.indexOf(' ', monthPos + 1);
            int endPos = pubdate.indexOf(' ', yearPos + 1);
            Calendar c1 = Calendar.getInstance();
            Calendar c2 = Calendar.getInstance();
            DateFormat monthFormat = new SimpleDateFormat("MMM", Locale.ENGLISH);
            Date dm = monthFormat.parse(pubdate
                    .substring(monthPos + 1, yearPos));

            c1.setTime(d);
            c2.setTime(dm);
            if (c1.get(Calendar.DAY_OF_MONTH) == Integer.parseInt(pubdate
                    .substring(dayPos + 1, monthPos))
                    && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                    && c1.get(Calendar.YEAR) == Integer.parseInt(pubdate
                            .substring(yearPos + 1, endPos))) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*
     * Given a String that follows a certain format,
     * returns a substring from it that contains a date.
     */
    private String getFileDate(String fileName) {
        try {
            return fileName.substring(0, 10);
        } catch (Exception e) {
            return null;
        }
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

    /* Load file to a string then return it. */
    private String getFileContent(String fileName, String encode) {
        StringBuilder sb = new StringBuilder();
        try {

            Scanner configScanner = new Scanner(new File(fileName), encode);
            while (configScanner.hasNextLine()) {
                sb.append(configScanner.nextLine());
            }

            configScanner.close();

        } catch (FileNotFoundException e) {
            logger.error("getFileContent(): " + "Failure to load file: {}",
                    fileName);
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }
}
