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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public abstract class RssScraper {

    protected final String OUTPUT_DATABASE_NAME = "rss.data";
    protected final String ID_COUNT_FILE_NAME = "idCount";
    protected final String ENCODE = "UTF-8";
    protected final String FOLDER_PATH_SEPERATOR = "/";

    protected String baseUrl;
    protected String dateString;
    protected String outputLocation;
    protected String rawDataDir;
    protected DateFormat dateFormat;
    protected int sentenceMinimumLength;

    protected Map<String, NewsData> dataMap;
    protected Set<String> duplicateChecker;

    /** List of all the categories from the config file. */
    protected List<String> categories;

    /** Mapping from a category name to a list of its RSS feeds. */
    protected Map<String, List<String>> rssCategoryToFeeds;

    protected Logger logger;
    protected Calendar calendar;
    protected Config config;

    public RssScraper(Calendar cal, Config con) {
        calendar = cal;
        config = con;
        logger = LoggerFactory.getLogger(this.getClass());
        readConfig();
    }

    /* Using the passed config file, set up some easy vars.
     * @modifies dateFormat, dateString, baseURl, sentenceMinimumLength
     */
    private void readConfig() {
        dateFormat = config.getDateFormat();
        dateString = dateFormat.format(calendar.getTime());
        baseUrl = config.getBaseUrl();
        sentenceMinimumLength = config.getSentenceMinimumLength();
    }

    /**
     * Fetch the raw news data from the RSS feeds specified in the Config file
     * and store it into the directory specified in the Config file.
     *
     * @requires config != null, logger != null, baseUrl != null, dateString != null
     * @modifies outputLocation, rawDataDir, categories, rssCategoryToFeeds, file system
     * @effects outputLocation, rawDataDir, categories: sets to what is specified in config
     * @effects rssCategoryToFeeds: fills map with rss feeds.
     * @effects file system: creates the directories specified in config: the root directory,
     *                       daily directory, and raw data directory. fills raw data directory
     *                       with fetched HTML data.
     */
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
                // try three times before we log an error
                for (int i = 0; i < 3; i++) {
                    try {
                        String url = constructUrl(categoryName, feedName);
                        Document doc = Jsoup.connect(url).get();

                        // write fetched xml to local data
                        BufferedWriter out = new BufferedWriter(
                                new OutputStreamWriter(new FileOutputStream(
                                new File(rawDataDir + dateString + "_"
                                + categoryName + "_" + feedName
                                + ".html"), true), ENCODE));
                        out.write(doc.toString());
                        out.close();

                        logger.info(
                                "fetchData(): " + "Fetched {}: {} successfully",
                                categoryName, feedName);
                        break;  // stop trying on success

                    } catch (IOException e) {
                        if (i < 2) continue;  // try again!
                        logger.error("fetchData(): Failed to download: {}_{}",
                            categoryName, feedName);
                        e.printStackTrace();
                    }
                }
            }
        }
        logger.info("fetchData(): End fetching.");
    }

    /**
     * This method defines how the rss feed URLs are constructed for each
     * news source.
     * @param feedName the name of the feed
     * @return an appropriate rss feed url.
     */
    public abstract String constructUrl(String category, String feed);

    /**
     * Take the fetched raw news data and process it into the JSON format.
     *
     * @param sourceDir where the raw news data is stored. If null, uses a
     *                  default daily directory instead.
     * @param targetDir where to store the process JSON files. If null, uses
     *                  a default daily directory instead.
     */
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

    /**
     * Takes the HTML files from the raw data directory and processes them.
     * @param dir the directory containing the raw HTML files
     * @param processOnly true if no scraping was done.
     * @modifies duplicateChecker, dataMap
     * @effects initializes and puts appropriate news data into them.
     */
    protected abstract void processHtml(String dir, boolean processOnly);

    /* Output the database to a JSON file. */
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
     * Create the directory for today's data. Returns the
     * name of the daily directory.
     * @require dateString is set.
     */
    protected String makeDailyDirectory(String rootDataFolder) {

        // make sure that the root directory exists. if not, create it.
        File folder = new File(rootDataFolder);
        if (!folder.exists())
            folder.mkdir();

        // make sure the dir for today exists. if not, create it.
        String dailyDir = rootDataFolder + dateString + "/";
        File todayFolder = new File(dailyDir);
        if (!todayFolder.exists())
            todayFolder.mkdir();

        // if the folder is not created, somehow
        if (!todayFolder.exists()) {
            logger.error("makeDailyDirectory(): "
                    + "Failure to create today's directory.");
            System.exit(1);
        }

        return dailyDir;
    }

    /*
     * Given a String that follows a certain format,
     * returns a substring from it that contains a date.
     */
    protected String getFileDate(String fileName) {
        try {
            return fileName.substring(0, 10);
        } catch (Exception e) {
            return null;
        }
    }

    /* Load file to a string then return it. */
    protected String getFileContent(String fileName, String encode) {
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

    /*
     * Check if the given date String is the same as dateString
     */
    protected boolean checkDateMatch(String pubdate) {
        if (pubdate.substring(0, 10).equals(dateString)) {
            return true;
        }
        Date d = null;
        try {
            d = dateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        int dayPos = pubdate.indexOf(' ');
        int monthPos = pubdate.indexOf(' ', dayPos + 1);
        int yearPos = pubdate.indexOf(' ', monthPos + 1);
        int endPos = pubdate.indexOf(' ', yearPos + 1);
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        DateFormat monthFormat = new SimpleDateFormat("MMM", Locale.ENGLISH);
        Date dm = null;

        try {
            dm = monthFormat.parse(pubdate.substring(monthPos + 1, yearPos));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        c1.setTime(d);
        c2.setTime(dm);
        if (c1.get(Calendar.DAY_OF_MONTH) == Integer.parseInt(pubdate
                .substring(dayPos + 1, monthPos))
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                && c1.get(Calendar.YEAR) == Integer.parseInt(pubdate
                        .substring(yearPos + 1, endPos))) {
            return true;
        }
        return false;
    }
}
