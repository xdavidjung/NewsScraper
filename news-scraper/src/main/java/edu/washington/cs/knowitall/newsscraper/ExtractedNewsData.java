package edu.washington.cs.knowitall.newsscraper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import edu.washington.cs.knowitall.extractor.conf.ReVerbOpenNlpConfFunction;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;

public class ExtractedNewsData extends NewsData {

    public Map<String, ChunkedBinaryExtraction> extractions;

    public ExtractedNewsData(String category, String subCategory, String title,
            String date) {
        super(category, subCategory, title, date);
        extractions = new HashMap<String, ChunkedBinaryExtraction>();
    }

    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t\n{");
        sb.append("\t\t" + getFieldsJson() + ", \n");
        sb.append("\t\t" + getExtractionsJsonString() + "\n");
        sb.append("}\n");
        return sb.toString();
    }

    // "extractions":[{"sent:":"content-of-the-extracted-string","arg1":"...",
    // "relation":"...", "arg2":"..."}, {...another extracted string..}, ..]
    private String getExtractionsJsonString() {
        assert extractions != null;
        StringBuilder sb = new StringBuilder();
        sb.append("\"extractions\":[");
        Iterator<Entry<String, ChunkedBinaryExtraction>> it = 
                extractions.entrySet().iterator();
        boolean empty = true;
        ReVerbOpenNlpConfFunction confFunc = null;
        try {
            confFunc = new ReVerbOpenNlpConfFunction();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        while (it.hasNext()) {
            empty = false;
            Map.Entry<String, ChunkedBinaryExtraction> pairs = 
                    (Map.Entry<String, ChunkedBinaryExtraction>) it.next();
            sb.append("\n\t\t\t{\"sent\":\"" +
                    pairs.getKey().replace("\"", "\\\"") + "\", \n");
            ChunkedBinaryExtraction cbe = pairs.getValue();

            // "arg1":"[arg1]",
            sb.append("\t\t\t\t\"arg1\":\"" +
                    cbe.getArgument1().toString().replace("\"", "\\\"") +
                    "\", \n");
            
            // "rArg1":"[range of arg1]"
            sb.append("\t\t\t\t\"rArg1\":\"" +
                    cbe.getArgument1().getRange().toString() + "\", \n");
            
            // "relation":"[rel]",
            sb.append("\t\t\t\t\"relation\":\"" +
                    cbe.getRelation().toString().replace("\"", "\\\"") +
                    "\", \n");
            
            // "rRel":"[range of rel]"
            sb.append("\t\t\t\t\"rRel\":\"" +
                    cbe.getRelation().getRange().toString() + "\", \n");
            
            // "arg2":"[arg2]",
            sb.append("\t\t\t\t\"arg2\":\"" +
                    cbe.getArgument2().toString().replace("\"", "\\\"") +
                    "\", \n");

            // "rArg2":"[range of arg2]"
            sb.append("\t\t\t\t\"rArg2\":\"" +
                    cbe.getArgument2().getRange().toString() + "\", \n");
            
            // "confidence":"[confidence]"
            sb.append("\t\t\t\t\"confidence\":\"" + 
                    confFunc.getConf(cbe) + "\"},");
        }
        if (!empty)
            sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

}
