
package sawim.history;

import sawim.comm.Util;



public class CachedRecord {

    public String text;
    public String date;
    public String from;
    public byte type; 
    private String shortText;

    public String getShortText() {
        if (null == shortText) {
            final int maxLen = 20;
            shortText = text;
            if (text.length() > maxLen) {
                shortText = text.substring(0, maxLen) + "...";
            }
            shortText = shortText.replace('\n', ' ').replace('\r', ' ');
        }
        return shortText;
    }

    public boolean isIncoming() {
        return 0 == type;
    }
}