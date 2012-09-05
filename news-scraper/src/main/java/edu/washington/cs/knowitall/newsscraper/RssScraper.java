package edu.washington.cs.knowitall.newsscraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public abstract class RssScraper {

    protected final String ID_COUNT_FILE_NAME = "idCount";
    protected final String ENCODE = "UTF-8";
    protected final String FOLDER_PATH_SEPERATOR = "/";

    protected String baseUrl;
    protected String dateString;
    protected String outputLocation;
    protected String rawDataDir;
    protected DateFormat dateFormat;
    protected int sentenceMinimumLength;

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
                try {
                    Document doc = Jsoup.connect(baseUrl + feedName).get();

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

                } catch (IOException e) {
                    logger.error("fetchData(): " + "Failed to download: {}_{}",
                            categoryName, feedName);
                    e.printStackTrace();
                }
            }
        }
        logger.info("fetchData(): End fetching.");
    }

    /**
     * Take the fetched raw news data and process it into the JSON format.
     *
     * @param sourceDir where the raw news data is stored. If null, uses a
     *                  default daily directory instead.
     * @param targetDir where to store the process JSON files. If null, uses
     *                  a default daily directory instead.
     */
    public abstract void processData(String sourceDir, String targetDir);

    /*
     * Create the directory for today's data. Returns the
     * name of the daily directory.
     * @require dateString is set.
     */
    private String makeDailyDirectory(String rootDataFolder) {

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
}
