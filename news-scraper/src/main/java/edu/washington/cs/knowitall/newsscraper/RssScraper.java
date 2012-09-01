package edu.washington.cs.knowitall.newsscraper;

public interface RssScraper {

    /**
     *
     * @param fetchData
     *            Whether to fetch and store data from the RSS.
     * @param processData
     *            Whether to process and store the stored RSS data.
     * @param sourceDir
     *            Where the scraped data is stored.
     * @param targetDir
     *            Where to store the processed data.
     */
    public void scrape(boolean fetchData, boolean processData, String sourceDir, String targetDir);

}
