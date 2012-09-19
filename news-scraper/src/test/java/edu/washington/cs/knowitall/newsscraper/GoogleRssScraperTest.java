package edu.washington.cs.knowitall.newsscraper;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class GoogleRssScraperTest {

    public static ArticleTextCleaner cleaner;

    @BeforeClass
    public static void setUp() throws Exception {
        cleaner = new ArticleTextCleaner();
    }

    public void testFixer(String str, String publisher, String expected) {
        String result = cleaner.clean(str, publisher);
        System.out.println(result);
        assertEquals(expected, result);
    }

    @Test
    public void testGeneralCases() {
        String s = "";

        testFixer(s, "", "");

        System.out.println();
    }

    @Test
    public void pastProblems() {
        String s1 = "By Hana Garrett-Walker and Matthew Backhouse A new genetic test that can predict the risk of developing autism is great progress towards more effective diagnosis of the condition, a local autism group says.";
        String s2 = "Edmonton - September 14, 2012 - The City has unveiled Edmonton's first bike box.";
        String s3 = "UNIVERSITY PARK, Pa. -- Penn State's endowment and similar funds increased by $24 million in fiscal year 2012, and the total market value reached a record high of $1.86 billion as of June 30, 2012.";
        String s4 = "By John P. Mello Jr., PCWorld Sep 14, 2012 8:07 AM GoDaddy announced Thursday 30 percent across-the-board discounts to its customers as an apology for an outage that knocked out the company's services for several hours this week.";
        String s5 = "By Andrew O'Hehir Deep in the closing credits to Paul Thomas Anderson's austere and challenging new film “The Master” you'll find some legalistic language of the sort attached to any Hollywood movie.";
        String s6 = "BY JUSTIN RAY Hoisting a clandestine prime payload -- likely a pair of formation-flying ocean surveillance satellites.";
        String s7 = "by Hilary George-Parkin | 3:31 pm, September 14th, 2012 As if we weren't already enough in awe of Diane von Furstenberg from a distance, this week we were fortunate to be in the designer's presence not one, not two, but three whole times.";
        String s8 = "By Anton Stanley | Friday, September 14, 2012 Carl Froch has warned Ricky Hatton that if he could get hurt if doesn't prepare himself properly for his return.";
        String s9 = "By JR Bookwalter September 14th 2012 Within an hour of Apple kicking off pre-orders for the new iPhone 5 at 12:01 am PDT Friday, shipping dates slipped to two weeks - and show no signs of improving any time soon. There were scattered reports of ...";
        String s10 = "DAMASCUS: International envoy Lakhdar Brahimi on Friday met with Syrian opposition figures who said he was bringing \"new ideas\" to peace efforts, as blasts rocked Damascus and regime air strikes targeted rebel areas in Aleppo.";
        String s11 = "By Latif Salman on 09/14/2012 09:25 PDT Recently, GoDaddy had to face rather critical network issues when millions of websites hosted by the company went down. Before soon, a member of the hacktivist group, Anonymous, claimed responsibility for the ...";
        String s12 = "By MIKE SIELSKI FLORHAM PARK, NJ—There is no such thing as a \"mild concussion\" in the NFL anymore. The Jets—and cornerback Darrelle Revis—learned this truth Friday. Though Revis participated in the non-contact portions of Friday's practice, ...";

        testFixer(s1, "", "A new genetic test that can predict the risk of developing autism is great progress towards more effective diagnosis of the condition, a local autism group says.");
        testFixer(s2, "", "The City has unveiled Edmonton's first bike box.");
        testFixer(s3, "", "Penn State's endowment and similar funds increased by $24 million in fiscal year 2012, and the total market value reached a record high of $1.86 billion as of June 30, 2012.");
        testFixer(s4, "PCWorld", "GoDaddy announced Thursday 30 percent across-the-board discounts to its customers as an apology for an outage that knocked out the company's services for several hours this week.");
        testFixer(s5, "", "Deep in the closing credits to Paul Thomas Anderson's austere and challenging new film “The Master” you'll find some legalistic language of the sort attached to any Hollywood movie.");
        testFixer(s6, "", "Hoisting a clandestine prime payload -- likely a pair of formation-flying ocean surveillance satellites.");
        testFixer(s7, "", "As if we weren't already enough in awe of Diane von Furstenberg from a distance, this week we were fortunate to be in the designer's presence not one, not two, but three whole times.");
        testFixer(s8, "", "Carl Froch has warned Ricky Hatton that if he could get hurt if doesn't prepare himself properly for his return.");
        testFixer(s9, "", "Within an hour of Apple kicking off pre-orders for the new iPhone 5 at 12:01 am PDT Friday, shipping dates slipped to two weeks - and show no signs of improving any time soon.");
        testFixer(s10, "", "International envoy Lakhdar Brahimi on Friday met with Syrian opposition figures who said he was bringing \"new ideas\" to peace efforts, as blasts rocked Damascus and regime air strikes targeted rebel areas in Aleppo.");
        testFixer(s11, "", "Recently, GoDaddy had to face rather critical network issues when millions of websites hosted by the company went down.");
        testFixer(s12, "", "There is no such thing as a \"mild concussion\" in the NFL anymore. The Jets—and cornerback Darrelle Revis—learned this truth Friday.");
    }
}
