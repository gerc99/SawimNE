

package sawim.util;

import sawim.comm.Config;

import java.util.Hashtable;
import java.util.Locale;

public class JLocale {
    
    public static String[] langAvailable;
    public static String[] langAvailableName;
    
    static private Hashtable resources = new Hashtable();
    private static String currentLanguage;

    public static void loadLanguageList() {
        Config config = new Config().load("/langlist.txt");
        langAvailable = config.getKeys();
        langAvailableName = config.getValues();
    }
    
    
    public static String getCurrUiLanguage() {
        return currentLanguage;
    }
    public static String getLanguageCode() {
        String country = getCurrUiLanguage();
        int separatorIndex = country.indexOf('_');
        if (-1 != separatorIndex) {
            country = country.substring(0, separatorIndex);
        }
        return country.toLowerCase();
    }
    public static String getSystemLanguage() {
        String lang = Locale.getDefault().toString();
        lang = ((null == lang) ? "" : lang).toUpperCase();
        for (int i = 0; i < langAvailable.length; ++i) {
            if (-1 != lang.indexOf(langAvailable[i])) {
                return langAvailable[i];
            }
        }
        return langAvailable[0];
    }
    public static boolean isCyrillic(String language) {
        return (-1 != language.indexOf("RU")) 
                || (-1 != language.indexOf("TT")) 
                || "UA".equals(language) 
                || "AD".equals(language)
                || "BE".equals(language); 
    }
    
    
    public static void setCurrUiLanguage(String currUiLanguage) {
        String language = JLocale.langAvailable[0];
        for (int i = 0; i < JLocale.langAvailable.length; ++i) {
            if (langAvailable[i].equals(currUiLanguage)) {
                language = langAvailable[i];
                break;
            }
        }
        currentLanguage = language;
        loadLang();
    }
    
    private static void loadLang(String lang) {
		Config config = new Config().load("/" + lang + ".lang");
		for (int j = 0; j < config.getKeys().length; ++j) {
			resources.put(config.getKeys()[j], config.getValues()[j]);
		}
    }
    private static void loadLang() {
        loadLang(currentLanguage);
        if (resources.isEmpty()) {
            loadLang("EN");
        }
    }
    
    
    public static String getString(String key) {
        if (null == key) return null;
        String value = (String) resources.get(key);
        return (null == value) ? key : value;
    }
    
    public static String getEllipsisString(String key) {
        return getString(key) + "...";
    }
}


