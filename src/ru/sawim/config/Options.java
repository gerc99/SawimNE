package ru.sawim.config;

import sawim.comm.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.TimeZone;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 25.12.12 15:30
 *
 * @author vladimir
 */
public class Options {
    private static final String OPTION_FILE = "sawimne.ini";
    private static final String OPTIONS_PREFIX = "OPTION_";

    public void store() {
        final IniBuilder sb = new IniBuilder();
        each(new Processor() {
            @Override
            public void process(String name, int key, Object value) {
                if (null == value) {
                    sb.comment();
                    value = getDefault(key);
                }
                sb.line(name, value);
            }
        });
        HomeDirectory.putContent(OPTION_FILE, sb.toString());
    }

    public void load() {
        Config config = new Config(HomeDirectory.getContent(OPTION_FILE));
        // reference to the actual config
        Object[] options = getOptionsArray();
        for (String opt : config.getKeys()) {
            try {
                int key = getOptionKey(opt);
                if (-1 < key) {
                    options[key] = toValue(key, config.getValue(opt));
                }
            } catch (Exception ignored) {
            }
        }
        setupSystem();
    }

    private void setupSystem() {
        int timeZone =  TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000;
        sawim.Options.setInt(sawim.Options.OPTION_GMT_OFFSET, timeZone);
        sawim.Options.setInt(sawim.Options.OPTION_LOCAL_OFFSET, 0);
    }


    private Object toValue(int key, String value) {
        if (key < 0) {
            return null;
        } else if (key < 64) {  /* 0-63 = String */
            return IniBuilder.extract(value);
        } else if (key < 128) {  /* 64-127 = int */
            return Integer.parseInt(value);
        } else if (key < 192) {  /* 128-191 = boolean */
            return Boolean.valueOf(value);
        } else if (key < 224) {  /* 192-223 = long */
            return Long.parseLong(value);
        }
        return null;
    }

    private Object getDefault(int key) {
        if (key < 0) {
            return null;
        } else if (key < 64) {  /* 0-63 = String */
            return "";
        } else if (key < 128) {  /* 64-127 = int */
            return 0;
        } else if (key < 192) {  /* 128-191 = boolean */
            return Boolean.FALSE;
        } else if (key < 224) {  /* 192-223 = long */
            return 0L;
        }
        return null;
    }


    private int getOptionKey(String name) {
        try {
            Class clazz = sawim.Options.class;
            return clazz.getField(OPTIONS_PREFIX + name.toUpperCase()).getInt(null);
        } catch (Exception e) {
            return -1;
        }
    }

    private void each(Processor p) {
        Class clazz = sawim.Options.class;
        Object[] options = getOptionsArray();
        for (Field field : clazz.getDeclaredFields()) {
            try {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod)) {
                    int key = field.getInt(null);
                    if (field.getName().startsWith(OPTIONS_PREFIX)) {
                        p.process(field.getName().replace(OPTIONS_PREFIX, ""), key, options[key]);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private Object[] getOptionsArray() {
        Object[] options = null;
        Class clazz = sawim.Options.class;
        try {
            Field optionsField = clazz.getDeclaredField("options");
            boolean accessible = optionsField.isAccessible();
            optionsField.setAccessible(true);
            options = (Object[]) optionsField.get(null);
            optionsField.setAccessible(accessible);
        } catch (Exception ignored) {
        }
        return options;
    }

    private static interface Processor {
        void process(String name, int key, Object value);
    }
}
