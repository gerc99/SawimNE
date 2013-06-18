package ru.sawim.config;

import sawim.comm.Config;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 28.12.12 15:21
 *
 * @author vladimir
 */
public class Templates {
    private static final String TEMPLATES_FILE = "templates.ini";

    public void store(Vector templates) {
        final IniBuilder sb = new IniBuilder();
        int num = 1;
        for (Object template : templates) {
            sb.line("" + num, template);
            num++;
        }
        HomeDirectory.putContent(TEMPLATES_FILE, sb.toString());
    }

    public Vector<String> load(Vector old) {
        Config config = new Config(HomeDirectory.getContent(TEMPLATES_FILE));
        Vector<String> result = new Vector<String>();
        for (String tpl : config.getValues()) {
            result.add(IniBuilder.extract(tpl));
        }
        if (null != old) for (Object tpl : old) {
            if (!result.contains(tpl)) result.add(tpl.toString());
        }
        return result;
    }
}
