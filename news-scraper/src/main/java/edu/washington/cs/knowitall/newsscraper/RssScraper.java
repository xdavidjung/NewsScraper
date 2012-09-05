package edu.washington.cs.knowitall.newsscraper;

public abstract class RssScraper {

    /**
     * Fetch the raw news data from the RSS feeds specified in the Config file
     * and store it into the directory specified in the Config file.
     */
    public abstract void fetchData();

    /**
     * Take the fetched raw news data and process it into the JSON format.
     *
     * @param sourceDir where the raw news data is stored. If null, uses a
     *                  default daily directory instead.
     * @param targetDir where to store the process JSON files. If null, uses
     *                  a default daily directory instead.
     */
    public abstract void processData(String sourceDir, String targetDir);
}
