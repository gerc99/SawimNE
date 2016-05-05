package ru.sawim.comm;

import android.content.res.Resources;
import ru.sawim.SawimApplication;

import java.util.Locale;

public class JLocale {

    public static String getString(int key) {
        try {
            return SawimApplication.getContext().getString(key);
        } catch (Resources.NotFoundException e) {
            return "";
        }
    }

    public static String getSystemLanguage() {
        return Locale.getDefault().toString().replace("_", "-").toLowerCase();
    }
}