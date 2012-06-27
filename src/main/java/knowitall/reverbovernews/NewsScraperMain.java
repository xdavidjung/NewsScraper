package knowitall.reverbovernews;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * This class is used to scrape news from the internet from the command line.
 * 
 * @author Pingyang He, David H Jung
 */
public class NewsScraperMain {

    /**
     * The name of the default local file that stores Config information. This
     * particular config file uses the Yahoo RSS feed.
     */
    public static final URL DEFAULT_CONFIG_URL = NewsScraperMain.class
            .getResource("YahooRssConfig");

    private static Logger logger;

    /**
     * Options that this class recognizes.
     */
    private static final String SCRAPE_DATA_ONLY = "s";
    private static final String SCRAPE_DATA_AND_PROCESS_DATA = "sp";
    private static final String PROCESS_RSS_WITH_GIVEN_DIR = "p";
    private static final String USE_REVERB = "r";
    private static final String USE_REVERB_WITH_DIR = "rd";
    private static final String FORMAT_OPT = "fmt";
    private static final String FORMAT_DIR = "fd";
    private static final String FORMAT_TODAY = "ftoday";
    private static final String FORMAT_TIME_FILTER = "ft";
    private static final String FORMAT_CONFIDENCE_THRESHOLD = "fct";
    private static final String FORMAT_CATEGORY_FILTER = "fc";
    private static final String USE_CONFIG_FILE = "c";
    private static final String HELP = "h";

    private static Calendar calendar;
    private static Options options;
    private static URL configUrl;

    public static void main(String[] args) throws IOException {
        initializeVars();

        CommandLine cmd = getCommands(args, options);

        validateOptionNumbers(cmd);

        fetchConfigFile(cmd);

        fetchNews(cmd);

        getExtractions(cmd);

        formatData(cmd);

        help(cmd);
    }

    /**
     * Initialize variables for this object.
     * 
     * @mofidies calendar, options.
     * @effects initializes them.
     */
    private static void initializeVars() {
        calendar = Calendar.getInstance();
        options = new Options();
        logger = LoggerFactory.getLogger(NewsScraperMain.class);
    }

    /**
     * Fetch news data from the RSS.
     * 
     * @param cmd
     *            CommandLine object containing the commands/options data.
     */
    private static void fetchNews(CommandLine cmd) {

        YahooRssScraper yrs = new YahooRssScraper(calendar, configUrl);

        // -s
        if (cmd.hasOption(SCRAPE_DATA_ONLY)) {
            yrs.scrape(true, false, null, null);

        // -sp
        } else if (cmd.hasOption(SCRAPE_DATA_AND_PROCESS_DATA)) {
            yrs.scrape(true, true, null, null);

        // -p
        } else if (cmd.hasOption(PROCESS_RSS_WITH_GIVEN_DIR)) {
            String[] dirs = cmd.getOptionValues(PROCESS_RSS_WITH_GIVEN_DIR);
            yrs.scrape(false, true, dirs[0], dirs[1]);

        } else {
            printUsage();
        }
    }

    /**
     * Pulls out extractions from the news data using ReVerb.
     * 
     * @throws IOException
     *             if the configuration file cannot be found.
     */
    private static void getExtractions(CommandLine cmd) throws IOException {
        ReverbNewsExtractor rne = new ReverbNewsExtractor(calendar, configUrl);

        // -r
        if (cmd.hasOption(USE_REVERB)) {
            rne.extract(null, null);

            // -rd
        } else if (cmd.hasOption(USE_REVERB_WITH_DIR)) {
            String[] dirs = cmd.getOptionValues(USE_REVERB_WITH_DIR);
            if (dirs != null && dirs.length == 2) {
                rne.extract(dirs[0], dirs[1]);
            } else {
                printUsage();
            }

        }
    }

    /**
     * Format the stored data - called when the user uses the -fmt option.
     * 
     * @throws IOException
     *             if the configuration file cannot be found.
     */
    private static void formatData(CommandLine cmd) throws IOException {
        if (cmd.hasOption(FORMAT_OPT)) {

            String[] dir = null;
            String[] timeInterval = null;
            // -1 indicates none
            double confidenceThreshold = -1;
            boolean formatToday = false;
            String category = null;

            // check for additional options used with fmt.
            if (cmd.hasOption(FORMAT_DIR)) {
                dir = cmd.getOptionValues(FORMAT_DIR);
                if (dir.length != 2 || dir[0] == null || dir[1] == null) {
                    printUsage();
                }
            }

            if (cmd.hasOption(FORMAT_TODAY)) {
                if (cmd.hasOption(FORMAT_TIME_FILTER))
                    printUsage();
                formatToday = true;

            } else if (cmd.hasOption(FORMAT_TIME_FILTER)) {
                timeInterval = cmd.getOptionValues(FORMAT_TIME_FILTER);
                if (timeInterval.length != 2 || timeInterval[0] == null
                        || timeInterval[1] == null) {
                    printUsage();
                }
            }

            if (cmd.hasOption(FORMAT_CONFIDENCE_THRESHOLD)) {
                confidenceThreshold = Double.parseDouble(cmd.getOptionValue(
                        FORMAT_CONFIDENCE_THRESHOLD, "-1"));
            }

            if (cmd.hasOption(FORMAT_CATEGORY_FILTER)) {
                category = cmd.getOptionValue(FORMAT_CATEGORY_FILTER);
                if (category == null)
                    printUsage();
            }

            ExtractedDataFormatter formatter = new ExtractedDataFormatter(
                    calendar, DEFAULT_CONFIG_URL);
            formatter.format(dir, timeInterval, confidenceThreshold, category,
                    formatToday);

        }
    }

    /*
     * Setup the command line options and parse the passed-in string array.
     */
    private static CommandLine getCommands(String[] args, Options options) {

        // -s
        Option fetchDataOnlyOp = new Option(SCRAPE_DATA_ONLY, false,
                "Fetch the RSS (without processing it).");

        // -sp
        Option fetchDataAndProcessData = new Option(
                SCRAPE_DATA_AND_PROCESS_DATA, false,
                "Fetch RSS and process it.");

        // -p
        Option processWithDirOp = new Option(
                PROCESS_RSS_WITH_GIVEN_DIR,
                false,
                "Process RSS only: the first arg is the source directory, the "
                        + "second arg is the target directory where data will be saved.");
        processWithDirOp.setArgs(2);

        // -r
        Option useReverbOp = new Option(USE_REVERB, false,
                "Use reverb to extract today's file.");

        // -rd
        Option useReverbWithDirOp = new Option(USE_REVERB_WITH_DIR, false,
                "Use reverb to extract files in the first arg and save it "
                        + "into second arg directory.");
        useReverbWithDirOp.setArgs(2);

        // -fmt
        Option formaterOp = new Option(FORMAT_OPT, false,
                "Format the reverb news database into a human readable file.");

        // -ftoday
        Option formatTodayOp = new Option(FORMAT_TODAY, false,
                "Format today's file; this can not be used with the "
                        + FORMAT_TIME_FILTER + " option.");
        // formaterOp.setArgs(2);

        // -fct
        Option formatConfidenceThreshhold = new Option(
                FORMAT_CONFIDENCE_THRESHOLD,
                false,
                "This option cannot be used without the "
                        + FORMAT_OPT
                        + " option. "
                        + "Specify a minimum confidence requirement; if not specified, "
                        + "then a default number of extractions will be taken.");
        formatConfidenceThreshhold.setArgs(1);

        // -fc
        Option formatCategoryFilter = new Option(
                FORMAT_CATEGORY_FILTER,
                false,
                "This option cannot be used without the "
                        + FORMAT_OPT
                        + " option. "
                        + "Specify the category name; if not specified, all categories "
                        + "will be used.");
        formatCategoryFilter.setArgs(1);

        // -ft
        Option formatTimeFilter = new Option(
                FORMAT_TIME_FILTER,
                false,
                "This option cannot be used without the "
                        + FORMAT_OPT
                        + " option. "
                        + "Specify the time interval. The files that fall into this "
                        + "interval will be formatted (for eg: 2012-05-01 2012-05-04); "
                        + "if not specified, then a default interval will be formatted.");
        formatTimeFilter.setArgs(2);

        // -fd
        Option formatDir = new Option(
                FORMAT_DIR,
                false,
                "This option cannot be used without the "
                        + FORMAT_OPT
                        + " option. "
                        + "Specify the directory of source files and a target file; if "
                        + "not specified, then a default will be used.");
        formatDir.setArgs(2);

        // -c
        Option useConfig = new Option(USE_CONFIG_FILE, false,
                "Specifies the absolute path to a config file to use. " +
                "eg. -c \"file:/dir/config/configFile\"");
        useConfig.setArgs(1);

        // -h
        Option helpOp = new Option(HELP, false, "print program usage");

        options.addOption(fetchDataOnlyOp);
        options.addOption(fetchDataAndProcessData);
        options.addOption(processWithDirOp);
        options.addOption(useReverbOp);
        options.addOption(useReverbWithDirOp);
        options.addOption(formaterOp);
        options.addOption(formatTodayOp);
        options.addOption(formatConfidenceThreshhold);
        options.addOption(formatCategoryFilter);
        options.addOption(formatTimeFilter);
        options.addOption(formatDir);
        options.addOption(useConfig);
        options.addOption(helpOp);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            printUsage();
        }
        return cmd;
    }

    /*
     * Initializes configFile.
     * 
     * @modifies configFile
     * 
     * @effects if the user has specified a configuration file, configFile is
     * assigned its URL; else, configFile is assigned to DEFAULT_CONFIG_FILE.
     */
    private static void fetchConfigFile(CommandLine cmd) {
        try {
            if (cmd.hasOption(USE_CONFIG_FILE)) {
                String configFileAbsPath = cmd.getOptionValue(USE_CONFIG_FILE);
                configUrl = new URL(configFileAbsPath);
            } else {
                configUrl = DEFAULT_CONFIG_URL;
            }

        } catch (IOException e) {
            logger.error("fetchConfigFile(): error loading config file.");
        }
    }

    /*
     * Validate the number of options passed in args
     */
    private static void validateOptionNumbers(CommandLine cmd) {
        if (cmd.getOptions().length == 0)
            printUsage();
    }

    /**
     * Outputs a help message.
     * 
     * @modifies nothing
     */
    private static void help(CommandLine cmd) {
        if (cmd.hasOption(HELP)) {
            printUsage();
        }
    }

    /*
     * Print usage and exit
     */
    private static void printUsage() {
        HelpFormatter f = new HelpFormatter();
        f.printHelp("options:", options);
        System.exit(1);
    }

}
