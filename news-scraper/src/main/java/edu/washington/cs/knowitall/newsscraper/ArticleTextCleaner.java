package edu.washington.cs.knowitall.newsscraper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArticleTextCleaner {

    protected Logger logger;

    private final Map<String, String> publishers;
    private final String GARBAGE_TAIL = "...";
    private final String MONTH = "(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER|January|February|March|April|May|June|July|August|September|October|November|December|JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)";
    private final String DAY = "(SUNDAY|MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUN|MON|TUE|WED|THU|FRI|SAT|Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sun|Mon|Tue|Wed|Thu|Fri|Sat)";
    private final String TIME_ZONE = "(ACDT|ACST|ACT|ADT|AEDT|AEST|AFT|AKDT|AKST|AMST|AMT|ART|AST|AWDT|AWST|AZOST|AZT|BDT|BIOT|BIT|BOT|BRT|BST|BTT|CAT|CCT|CDT|CEDT|CEST|CET|CHADT|CHAST|CHOT|ChST|CHUT|CIST|CIT|CKT|CLST|CLT|COST|COT|CST|CT|CVT|CWST|CXT|DAVT|DDUT|DFT|EASST|EAST|EAT|ECT|ECT|EDT|EEDT|EEST|EET|EGST|EGT|EIT|EST|ET|FET|FJT|FKST|FKT|FNT|GALT|GAMT|GET|GFT|GILT|GIT|GMT|GST|GYT|HADT|HAEC|HAST|HKT|HMT|HOVT|HST|ICT|IDT|I0T|IRDT|IRKT|IRST|IST|JST|KGT|KOST|KRAT|KST|LHST|LINT|MAGT|MART|MAWT|MDT|MET|MEST|MHT|MIST|MIT|MMT|MSK|MST|MST|MST|MUT|MVT|MYT|NCT|NDT|NFT|NPT|NST|NT|NUT|NZDT|NZST|OMST|ORAT|PDT|PET|PETT|PGT|PHOT|PHT|PKT|PMDT|PMST|PONT|PST|RET|ROTT|SAKT|SAMT|SAST|SBT|SCT|SGT|SLT|SRT|SST|SYOT|TAHT|THA|TFT|TJT|TKT|TLT|TMT|TOT|TVT|UCT|ULAT|UTC|UYST|UYT|UZT|VET|VLAT|VOLT|VOST|VUT|WAKT|WAST|WAT|WEDT|WEST|WET|WST|YAKT|YEKT)";

    // Dàvid-Hanson T. D'MacMcO'Diego-Sanchez Jr.
    private final String LOWERCASE_NAME = "[A-Z]([A-Z])?[a-z'à-ÿ]*(-[A-Z][a-z'à-ÿ]+)? (de )?(del )?([A-Z](\\.)? )?(Mac)?(D')?(Mc)?(O')?[A-Z][a-z'à-ÿ]+(-[A-Z][a-z'à-ÿ]+)?( (Jr|Sr)(\\.)?)?";
    private final String UPPERCASE_NAME = "[A-ZÀ-Ý]+(-[A-ZÀ-Ý]+)? ([A-Z](\\.)? )?(Mc)?(O')?[A-ZÀ-Ý]+(-[A-ZÀ-Ý]+)?( (J[Rr]|S[Rr])(\\.)?)?";
    private final String DASH = "(- -|--|-+|―+|—+|–+|‒+|‑+|‐+|•+|\\|)";


    protected ArticleTextCleaner() {
        logger = LoggerFactory.getLogger(this.getClass());
        publishers = fillPublishers();
    }

    /* Given the text of an article preview, removes "chrome" such as author,
     * publisher, and date from it. If no chrome is found, returns the argument
     * string.
     * @param descText the text of an article preview
     * @param publisher the publisher of the article - used for logging.
     * @return descText sans the "chrome".
     */
    protected String clean(String descText, String publisher) {
        descText = clean(descText, publisher, true);

        // if cleaned text starts with lowercase, scrap the first sentence
        if (Pattern.matches("^[a-z].*", descText) && !descText.startsWith("iP")) {
            int endOfFirstSentence = Math.max(descText.indexOf(". "),
                    Math.max(descText.indexOf("? "), descText.indexOf("! ")));
            if (endOfFirstSentence == -1) return "";
            descText = descText.substring(endOfFirstSentence + 2, descText.length());
        }

        // check for double-backslash problems:
        descText.replace("\\\\\"", "\\\"");

        return descText;
    }

    protected String clean(String descText, String publisher, boolean first) {
        if (descText.equals("")) return "";

        if (!publishers.containsKey(publisher)) {
            // logger.info("Publisher not found: " + publisher);
            return fixGeneralContent(descText);
        }
        Pattern p = Pattern.compile(publishers.get(publisher));
        Matcher m = p.matcher(descText);
        boolean found = m.find();

        if (found) {
            return clean(descText.replaceFirst(publishers.get(publisher), ""), publisher, false);
        } else if (first) {
            // logger.info("Pattern not found for publisher: " + publisher + " for text: " + descText + ".");
        }

        return fixGeneralContent(descText);
    }

    /*
     * Fixes problems that aren't publisher-specific.
     */
    private String fixGeneralContent(String descText) {
        if (descText.equals("")) return "";

        String original = descText;

        if (descText.endsWith(GARBAGE_TAIL)) {
            int endOfLastSentence = Math.max(descText.lastIndexOf(". "),
                    Math.max(descText.lastIndexOf("? "),
                    Math.max(descText.lastIndexOf("! "),
                    Math.max(descText.lastIndexOf("• "),
                             descText.lastIndexOf("* ")))));
            descText = descText.substring(0, endOfLastSentence + 1);
        }

        // JavaScript text
        descText = descText.replaceFirst(".*Hello, you either have JavaScript turned off or an old version of Adobe's Flash Player. ", "");

        // Authors
        descText = descText.replaceFirst("^[bB][yY] " + LOWERCASE_NAME + "( (and|AND|And) " + LOWERCASE_NAME + ")*( " + DASH + ")?(\\.| \\||,|:)? ", ""); // By David Jung-Jung and David Jung (at beginning)
        descText = descText.replaceFirst("^[bB][yY] " + UPPERCASE_NAME + "( (and|AND|And) " + UPPERCASE_NAME + ")*( " + DASH + ")?(\\.| \\||,|:)? ", ""); // BY DAVID JUNG

        // Specific source-independent tags.
        descText = descText.replaceFirst("^AP ((Sports|Photo)( |/))?(\\| )?", ""); // AP
        descText = descText.replaceFirst(".*/CNW/ - ", ""); // CNW

        // Sources like (AP), (Businessweekly) followed by a dash
        descText = descText.replaceFirst(".*\\([A-Z]+.*\\) " + DASH + " ", "");
        descText = descText.replaceFirst("^(\\([A-Za-z0-9\\. ]+\\)(\\.)?( |" + DASH + "))", ""); // ^(SOURCE)( |-)
        descText = descText.replaceFirst("^[A-Za-z]+( " + DASH + "|:) ", "");

        // Date and time together:
        descText = descText.replaceFirst(".*[0-9]{1,2}/[0-9]{1,2}/(20)?[0-9]{2} [0-9]{1,2}:[0-9]{1,2} ([aApP][mM])?( " + TIME_ZONE + ")?(\\.|,|:)? (" + DASH + " )?", ""); // 12/12/12 12:12 pm
        descText = descText.replaceFirst("^([Oo][Nn] )?" + DAY + "(,) [0-9]{1,2}/[0-9]{1,2}/(20)?[0-9]{2} - [0-9]{1,2}:[0-9]{1,2} ([aApP][mM])?", ""); // Fri(,) 12/12/2012 - 12:12( pm| am)

        // Dates:
        descText = descText.replaceFirst("^([pP]osted )?([oO][nN] )?(" + DAY + "(,) )?[0-9]{1,2}/[0-9]{1,2}/(20)?[0-9]{2} (" + DASH + " )?", ""); // (Posted )(On )12/12/(20)12
        descText = descText.replaceFirst("^(Posted )?(on )?(" + DAY + "(,) )?[0-9]{1,2}([tT][hH])? " + MONTH + "(,)? (20)?[0-9]{2}(\\.|,)? (" + DASH + " )?", ""); // ^9 Sep, 2012
        descText = descText.replaceFirst("^(Posted )?(on )?(" + DAY + "(,) )?" + MONTH + " [0-9]{1,2}([tT][hH])?(,)? (20)?[0-9]{2}(\\.|,)? (" + DASH + " )?", ""); // ^Sep 9(th), 2012
        descText = descText.replaceFirst(".*[0-9]{1,2}([tT][hH])? " + MONTH + "(,)? (20)?[0-9]{2}(\\.|,)? " + DASH + " ", ""); // 9 Sep, 2012 -
        descText = descText.replaceFirst(".*" + MONTH + " [0-9]{1,2}([tT][hH])?(,)? (20)?[0-9]{2}(\\.|,)? " + DASH + " ", ""); // Sep 9(th), 2012 -
        descText = descText.replaceFirst("^" + MONTH + " [0-9]{1,2}(,)?( (20|')[0-9]{2})?(:)?( " + DASH + " )?( )?", "");

        // Times
        descText = descText.replaceFirst("^[0-9]{1,2}:[0-9]{1,2}( )?([pPaA][mM])? " + TIME_ZONE + "(\\.)? (" + DASH + " )?", ""); // 12:12 pm ET(.)
        descText = descText.replaceFirst("^[0-9]{1,2}:[0-9]{1,2}( )?([pPaA][mM])?(\\.|,)?( )?(" + DASH + " )?", ""); // 12:12 pm (at beginning)

        // Cities
        descText = descText.replaceFirst("^[A-Z\\.']+( [A-Z\\.']+)?: ", ""); // ^DAMASCUS:
        descText = descText.replaceFirst("^[A-Z\\.']+( [A-Z\\.']+)?" + DASH, ""); // ^MONTREAL-
        descText = descText.replaceFirst(".*([A-Z\\.']+ )+" + DASH + "( )?", ""); // LOS ANGELES -
        descText = descText.replaceFirst(".*([A-Z\\.']+ )*([A-Z\\.']+), ([A-Z][a-z\\.]+ )+" + DASH + " ", ""); // AUSTIN, Texas|Ill. -
        descText = descText.replaceFirst("^([A-Z\\.']+ )*([A-Z\\.']+), [A-Z\\.]+( [A-Z\\.]+)*" + DASH, ""); // ^AUSTIN, TEX REX.-

        if (original.equals(descText)) return descText;

        return fixGeneralContent(descText);
    }


    /* Returns a mapping from publisher to "cleaner" regex. */
    private Map<String, String> fillPublishers() {
        Map<String, String> pubs = new HashMap<String, String>();
        URL publishersUrl = NewsScraperMain.class.getResource("GooglePublishers.csv");
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(publishersUrl.openStream(), "UTF-8"));
            String nextLine;
            while((nextLine = br.readLine()) != null) {
                String[] pubWithRegex = nextLine.split("\t");
                String publisherName = pubWithRegex[0];
                String regex = pubWithRegex[1];

                pubs.put(publisherName, regex);
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return pubs;
    }
}
