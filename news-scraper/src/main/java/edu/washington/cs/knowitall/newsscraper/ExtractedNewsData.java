package edu.washington.cs.knowitall.newsscraper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import edu.washington.cs.knowitall.extractor.ReVerbExtractor;
import edu.washington.cs.knowitall.extractor.conf.ReVerbOpenNlpConfFunction;
import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;

/**
 * This class adds extraction data to News Data.
 * 
 * @author Pingyang He, David Jung
 *
 */
public class ExtractedNewsData extends NewsData {

    public Map<String, ChunkedSentence> extractions;
    private ReVerbExtractor reverb;
    
    public ExtractedNewsData(String category, String subCategory, String title,
            String date) {
        super(category, subCategory, title, date);
        extractions = new HashMap<String, ChunkedSentence>();
        reverb = new ReVerbExtractor();
    }

    public String toJsonString(ReVerbExtractor re) {
        if (reverb == null) reverb = re;
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
        Iterator<Entry<String, ChunkedSentence>> it = 
                extractions.entrySet().iterator();
        boolean empty = true;
        ReVerbOpenNlpConfFunction confFunc = null;
        try {
            confFunc = new ReVerbOpenNlpConfFunction();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        // iterate over each chunked sentence
        while (it.hasNext()) {
            Map.Entry<String, ChunkedSentence> pair = 
                    (Map.Entry<String, ChunkedSentence>) it.next();
            String sentString = pair.getKey();
            ChunkedSentence cs = pair.getValue();
            if (reverb == null) reverb = new ReVerbExtractor();
            Iterable<ChunkedBinaryExtraction> cbes = reverb.extract(cs);
            
            // iterate over each extraction from the sentence
            for(ChunkedBinaryExtraction cbe : cbes) {
                empty = false;
                sb.append("\n\t\t\t{\"sent\":\"" +
                        sentString.replace("\"", "\\\"") + "\", \n");

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
                
                // "chunkTags":"[chunk tags]"
                sb.append("\t\t\t\t\"chunkTags\":\"" +
                        cs.getChunkTagsAsString() + "\", \n");
                
                // "posTags":"[pos tags]"
                sb.append("\t\t\t\t\"posTags\":\"" +
                        cs.getPosTagsAsString() + "\", \n");
                
                // "offsets":"[offsets]"
                sb.append("\t\t\t\t\"offsets\":\"" +
                        cs.getOffsetsAsString() + "\", \n");
                
                // "confidence":"[confidence]"
                sb.append("\t\t\t\t\"confidence\":\"" + 
                        confFunc.getConf(cbe) + "\"},");
            }
        }
        if (!empty)
            sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

}
