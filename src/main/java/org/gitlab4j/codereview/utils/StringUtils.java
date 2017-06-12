package org.gitlab4j.codereview.utils;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    /**
     * Get a List of String derived from a delimited string
     * 
     * @param listString
     * @param delimeter
     * @return
     */
    public static final List<String> getListFromString(String listString, String delimeter) {

        if (listString == null) {
            return (null);
        }

        listString = listString.trim();
        if (listString.length() == 0) {
            return (null);
        }

        String[] stringArray = listString.split(delimeter, -1);
        ArrayList<String> list = new ArrayList<String>(stringArray.length);
        for (String s : stringArray) {
            s = s.trim();
            if (s.length() > 0) {
                list.add(s);
            }
        }

        if (list.size() == 0) {
            return (null);
        }

        return (list);
    }
    
    public static final String buildUrlString(String ... urlItems) {
        
        if (urlItems == null || urlItems.length == 0)
            return ("");
        
        StringBuilder urlString = new StringBuilder();
        for (String urlItem : urlItems) {
          
            if (urlItem != null) {
              
               urlItem = urlItem.trim();
               if (urlItem.length() > 0) {
               
                   int length = urlString.length();
                   if (length > 0 && urlItem.charAt(0) != '/' && urlString.charAt(length - 1) != '/')
                       urlString.append('/');
        
                   urlString.append(urlItem);
               }
            }
        }
        
        return (urlString.toString());
    }
}
