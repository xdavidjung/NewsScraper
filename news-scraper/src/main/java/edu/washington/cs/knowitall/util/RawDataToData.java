package edu.washington.cs.knowitall.util;

import java.io.File;
import java.util.Calendar;

import edu.washington.cs.knowitall.newsscraper.Config;
import edu.washington.cs.knowitall.newsscraper.NewsScraperMain;
import edu.washington.cs.knowitall.newsscraper.YahooRssScraper;


public class RawDataToData {

    public static void main(String[] args){
        File root = new File("yahoo_data/recover/");
        String[] folders = root.list();

        for(String folderName : folders) {
            YahooRssScraper yrs = new YahooRssScraper(Calendar.getInstance(),
                    new Config(NewsScraperMain.class.getResource("YahooRssConfig")));
            yrs.processData("yahoo_data/recover/" + folderName + "/raw_data/", "yahoo_data/recover/" + folderName);
        }

    }
}
