package edu.washington.cs.knowitall.newsscraper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This class is used to read the JSON config files for news-scraper
 * and holds the information for easy access.
 * @author Pingyang He, David H Jung
 *
 */
public class Config {

    private Logger logger;

    private static final String DATE_FORMAT_STRING = "yyyy-MM-dd";

    // these are all fields in the json config file.
    private static final String FOLDER_NAME = "root-dir";
    private static final String RSS_URL = "rss-url";
    private static final String CATEGORY_LIST = "categories";
    private static final String RSS_FEED_LIST = "rss-list";
    private static final String SENTENCE_MINIMUM_LENGTH = "sentence-minimum-length";
    private static final String EXTRACTED_DATA_SUFFIX = "extracted_data_suffix";
    private static final String EXTRACTED_DIR = "extracted_data_dir";
    private static final String FORMATTED_EXTRACTED_DATA = "formatted_extracted_data_dir";

    /** Holds all the information in the JSON config file. */
    private JsonObject configJson;

    /** How dates are formatted. */
    private DateFormat dateFormat;

    /** The directory where everything will be stored - must end in "/". */
    private String rootDir;

    /** The base url of the RSS feed. */
    private String baseUrl;

    private String extractedDataSuffix;

    /** The directory to place extracted data. */
    private String extractedDataDir;

    /** The directory to place formatted extracted data. */
    private String formattedExtractedDataDir;

    /** The list of all the categories in the RSS feed. */
    private List<String> categoryList;

    /** Loads the given URL and stores the information stored in the
     *  configuration file in this object.
     *  @param configUrl the url of the configuration file to load.
     *  @throws IllegalArgumentException if configUrl is not a properly-
     *                                   formatted json file.
     */
    public Config(URL configUrl) {

        logger = LoggerFactory.getLogger(YahooRssScraper.class);
        dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);

        try {
            loadConfig(configUrl);
        } catch (IOException e) {
            logger.error("loadConfig(): failed to load config file: {}.",
                          configUrl.toString());
            throw new IllegalArgumentException("Unable to load configuration file.");
        }
    }

    /*
     * Load the configuration file with given file location
     * @param configUrl is the location of the configuration file
     * @throws IOException
     */
    private void loadConfig(URL configUrl) throws IOException{

        String fileContent = readFile(configUrl);

        configJson = (JsonObject)(new JsonParser()).parse(fileContent);
        rootDir = configJson.get(FOLDER_NAME).getAsString();
        baseUrl = configJson.get(RSS_URL).getAsString();

        extractedDataSuffix = configJson.get(EXTRACTED_DATA_SUFFIX).getAsString();
        extractedDataDir = configJson.get(EXTRACTED_DIR).getAsString();
        formattedExtractedDataDir = configJson.get(FORMATTED_EXTRACTED_DATA).getAsString();
        generateCategoryList();
    }

    /*
     * This reads the category list from the json config file and stores
     * them as Strings in the categoryList variable.
     */
    private void generateCategoryList() {
        categoryList = new ArrayList<String>();
        JsonArray categoryJA = configJson.get(CATEGORY_LIST).getAsJsonArray();
        for(int i = 0; i < categoryJA.size(); i++){
            categoryList.add(categoryJA.get(i).getAsString());
        }
    }

    /** @return get the data format type, eg yyyy-MM-dd */
   public String getDateFormatString(){
       return DATE_FORMAT_STRING;
   }

    /** @return the dataFormat to use. */
    public DateFormat getDateFormat(){
        return dateFormat;
    }

    /** @return the instance of the current configuration. */
    public JsonObject getConfig() {
        return configJson;
    }

    /** @return the directory of root file */
    public String getRootDir() {
        return rootDir;
    }

    /** @return the base url of the RSS feed */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** @return a list of categories in string format */
    public List<String> getCategories() {
        return categoryList;
    }

    /** @return the list of rss category names as a jsonArray. */
    public JsonArray getJsonCategories() {
        return configJson.get(CATEGORY_LIST).getAsJsonArray();
    }

    /** @return the mapping from rss categories to rss feeds as a jsonObject. */
    public JsonObject getJsonFeeds() {
        return configJson.get(RSS_FEED_LIST).getAsJsonObject();
    }

    /** @return the sentence minimum length. */
    public int getSentenceMinimumLength() {
        return configJson.get(SENTENCE_MINIMUM_LENGTH).getAsInt();
    }

    /** @return the extracted data suffix */
    public String getExtractedDataSuffix() {
        return extractedDataSuffix;
    }

    /** @return the place where extracted data is stored */
    public String getExtractedDataDir() {
        return extractedDataDir;
    }

    /** @return where the formatted data will be stored */
    public String getFormattedExtractedDataDir() {
        return formattedExtractedDataDir;
    }

    /* read the given file into memory as a string */
    private String readFile(URL configUrl) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(configUrl.openStream()));
        StringBuilder sb = new StringBuilder();
        String nextLine;
        while((nextLine = br.readLine()) != null)
            sb.append(nextLine);

        br.close();
        return sb.toString();
    }
}
