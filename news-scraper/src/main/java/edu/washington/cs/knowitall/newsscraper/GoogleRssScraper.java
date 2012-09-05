package edu.washington.cs.knowitall.newsscraper;

import java.util.Calendar;

public class GoogleRssScraper extends RssScraper {

    public GoogleRssScraper(Calendar cal, Config con) {
        super(cal, con);
    }

    @Override
    public void fetchData() {

        /*
         * Some notes on the Google RSS:
         * the base url is http://news.google.com/news?output=rss
         * some parameters to pass:
         *  ned: nation edition. ned=us, ned=uk, ned=ca, etc
         *  hl: language. hl=en
         *  topic: the topic we want.
         *      h: top news
         *      s: sports
         *      n: nation
         *      b: business
         *      tc: technology
         *      m: health
         *      e: entertainment
         *      snc: science
         *      el: elections
         *      p: politics
         */
    }

    @Override
    public void processData(String sourceDir, String targetDir) {
        // TODO Auto-generated method stub

    }

}
