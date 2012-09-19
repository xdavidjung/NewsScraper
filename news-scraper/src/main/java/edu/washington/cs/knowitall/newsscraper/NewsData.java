package edu.washington.cs.knowitall.newsscraper;

import java.lang.reflect.Field;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Pingyang He
 *
 */
public class NewsData {
    public String title;
    public String date;
    public String imgAlt;
    public String imgTitle;
    public String content;
    public String category;
    public String subCategory;
    public String url;
    public String source;
    public String imgUrl;

    public NewsData(String category, String subCategory, String title,
            String date) {
        imgAlt = "";
        content = "";
        imgTitle = "";
        url = "";
        source = "";
        imgUrl = "";
        this.category = category;
        this.subCategory = subCategory;
        this.title = title;
        this.date = date;
    }

    /**
     *
     * @return a JSONObject contains all the fields of this class
     */
    public JSONObject toJSONObject() {
        JSONObject jObject = new JSONObject();
        try {
            Field[] fields = this.getClass().getFields();
            for (Field field : fields) {
                jObject.put(field.getName(), field.get(this));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return jObject;
    }

    /**
     * returns this object as json string
     *
     * @return the json string that represents this object
     */
    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(getFieldsJson());
        sb.append("}");
        return sb.toString();
    }

    // return json info of fields in this class
    protected String getFieldsJson() {
        StringBuilder sb = new StringBuilder();
        Field[] fields = this.getClass().getFields();
        String seperator = ", ";

        for (Field field : fields) {
            String fieldName = field.getName();
            if (!fieldName.equals("extractions")) {
                sb.append("\"" + field.getName() + "\"");
                sb.append(":");
                String fieldVal = null;
                try {
                    fieldVal = field.get(this).toString();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    fieldVal = "";
                }
                sb.append("\""
                        + fieldVal.replace("\"", "\\\"")
                        + "\"");
                sb.append(seperator);
            }
        }
        // fence-post problem
        sb.delete(sb.length() - seperator.length(), sb.length());
        return sb.toString();
    }

}
