package edu.washington.cs.knowitall.newsscraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.washington.cs.knowitall.extractor.ReVerbExtractor;
import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.OpenNlpSentenceChunker;
import edu.washington.cs.knowitall.util.DefaultObjects;

/**
 * This class gets ReVerb extractions from news datum and then outputs
 * JSON files containing the extraction data.
 *
 * @author Pingyang He, David Jung
 */
public class ReverbNewsExtractor {

    // private final String TARGET_DIR = "reverb_extracted";
    private final String ENCODE = "UTF-8";

    private Logger logger;

    private Config config;
    private String dateString;
    private String rootDir;
    private String extractedDataSuffix;
    private String extractedDataDir;
    private String tag;
    private Calendar calendar;
    private Map<Long, ExtractedNewsData> data;
    private OpenNlpSentenceChunker chunker;
    private ReVerbExtractor reverb;

    /**
     * @param calendar
     *            gives the time of the caller
     * @param configFileName
     *            tells the location of configuration file. if null, uses a
     *            default.
     */
    public ReverbNewsExtractor(Calendar cal, Config con) {
        logger = LoggerFactory.getLogger(ReverbNewsExtractor.class);

        reverb = new ReVerbExtractor();
        calendar = cal;
        config = con;
        data = new HashMap<Long, ExtractedNewsData>();
        try {
            chunker = new OpenNlpSentenceChunker();
        } catch (IOException e) {
            logger.error("Constructor: unable to initialize sentence chunker.");
            logger.error("{}", e);
        }
    }

    /**
     * Get extractions from the data.
     *
     * @param srcDir
     *            specify the location of source data, if null, then use today's
     *            location
     * @param targetDir
     *            specify where the result will be stored, if null, then put
     *            data in today's location
     */
    public void extract(String srcDir, String targetDir) {

        loadConfig();

        logger.info("Constructor: Preparing to extract news data.");

        String location = null;
        if (srcDir == null && targetDir == null) {
            // extract from the default location
            location = rootDir + dateString + "/data/";
        } else if (srcDir != null && targetDir != null) {
            location = srcDir;
        } else {
            logger.error("extract(): bad directories.");
        }

        if (!location.endsWith("/"))
            location += "/";

        logger.info("extract(): Location: {}", location);
        File dataFolder = new File(location);
        String[] dataFiles = dataFolder.list();
        if (dataFiles == null) {
            logger.error("extract(): Can't load {}.", location);
            return;
        }
        for (String fileName : dataFiles) {
            extractData(loadData(location + fileName));

            // start outputting data
            if (targetDir == null) {
                outputData(extractedDataDir + "/");
            } else {
                dateString = fileName.substring(0, 10);
                if (!targetDir.endsWith("/"))
                    targetDir += "/";
                outputData(targetDir);
            }
            data.clear();
        }

        logger.info("Extraction finished.");
    }

    /*
     * parse the given newsData json string and extract the info in the string
     */
    private void extractData(String newsData) {
        logger.info("extractData(): Starting extraction.");
        Gson gson = new Gson();
        HashMap<Long, ExtractedNewsData> map = gson.fromJson(newsData,
                new TypeToken<HashMap<Long, ExtractedNewsData>>() {
                }.getType());
        Iterator<Entry<Long, ExtractedNewsData>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, ExtractedNewsData> pairs = it
                    .next();
            ExtractedNewsData currentData = pairs
                    .getValue();
            currentData.extractions = new HashMap<String, ChunkedSentence>();
            reverbExtract(currentData, currentData.imgAlt);
            reverbExtract(currentData, currentData.imgTitle);
            reverbExtract(currentData, currentData.content);
            reverbExtract(currentData, currentData.title);
        }
        // add this map to the wholeData
        data.putAll(map);
    }

    /*
     *
     */
    private void outputData(String targetDir) {
        logger.info("outputData(): Starting to output data.");

        File targetFolder = new File(targetDir);
        if (!targetDir.endsWith("/"))
            targetDir += "/";
        targetFolder.mkdirs();
        if (!targetFolder.exists())
            logger.error("outputData(): can't create folder.");
        String jsonDataDir = targetDir + dateString + "_" + tag + "_ExtractedData."
                + extractedDataSuffix;
        // String readableDataDir = targetDir + dateString + "_readable.txt";
        logger.info("outputData(): storing in {}", targetDir);
        File jsonDataFile = new File(jsonDataDir);

        try {
            jsonDataFile.createNewFile();
        } catch (IOException e) {
            logger.error("outputData(): Unable to create new jsonDataFile.");
            logger.error("{}", e);
        }

        if (!jsonDataFile.exists())
            logger.error("outputData(): can't create output file data.");
        BufferedWriter out = null;

        try {
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(jsonDataFile), ENCODE));
        } catch (Exception e) {
            logger.error("outputData(): Unable to create new Buffered"
                    + "Writer object.");
            logger.error("{}", e);
        }

        Iterator<Entry<Long, ExtractedNewsData>> it = data.entrySet()
                .iterator();

        // create JSON output string manually
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        String seperator = ", ";
        boolean empty = true;

        while (it.hasNext()) {
            empty = false;
            Map.Entry<Long, ExtractedNewsData> pair = it
                    .next();
            sb.append(pair.getValue().toJsonString(reverb) + seperator);
        }

        if (!empty)
            sb.delete(sb.length() - seperator.length(), sb.length());
        sb.append("]");
        try {
            out.write(sb.toString());
            out.close();
        } catch (IOException e) {
            logger.error("outputData(): Error writing to/closing BufferedWriter.");
            logger.error("{}", e);
        }
    }

    /*
     * extract the given string, and store the extracted information into the
     * given ExtractedNewsData
     */
    private void reverbExtract(ExtractedNewsData currentData, String str) {
        if (str != null && str.length() > 1) {
            try {
                String[] sentences = DefaultObjects
                        .getDefaultSentenceDetector().sentDetect(str);

                for (String sent : sentences) {
                    currentData.extractions.put(sent, chunker.chunkSentence(sent));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Load file with given location and return file content.
     */
    private String loadData(String location) {
        logger.info("loadData(): Loading data from: {}", location);
        StringBuilder sb = new StringBuilder();
        File dataFile = new File(location);
        Scanner sc = null;

        try {
            sc = new Scanner(dataFile, ENCODE);

            while (sc.hasNextLine())
                sb.append(sc.nextLine());

        } catch (FileNotFoundException e) {
            logger.error("loadData(): Unable to load data.");
            logger.error("{}", e);
        }

        if (sc != null) sc.close();

        return sb.toString();
    }

    /*
     * load configuration file from given location and name
     */
    private void loadConfig() {
        rootDir = config.getRootDir();
        dateString = config.getDateFormat().format(calendar.getTime());
        extractedDataSuffix = config.getExtractedDataSuffix();
        extractedDataDir = config.getExtractedDataDir();
        tag = config.getTag();
    }

}
