package ru.sawim.models.list;

import android.graphics.Bitmap;
import ru.sawim.Scheme;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.icons.Icon;

import java.util.ArrayList;
import java.util.List;


public final class VirtualListModel {
    public List<VirtualListItem> elements = new ArrayList<VirtualListItem>();
    private String header = null;

    public final void addPar(VirtualListItem item) {
        elements.add(item);
    }

    public void clear() {
        elements.clear();
        header = null;
    }

    public void removeFirstText() {
        elements.remove(0);
    }

    public final VirtualListItem createNewParser(boolean itemSelectable) {
        return new VirtualListItem(itemSelectable);
    }

    public final void addItem(String text, boolean active) {
        byte type = active ? Scheme.FONT_STYLE_BOLD : Scheme.FONT_STYLE_PLAIN;
        VirtualListItem item = createNewParser(true);
        item.addDescription(text, Scheme.THEME_TEXT, type);
        addPar(item);
    }

    public final void setHeader(String header) {
        this.header = header;
    }

    public final void setHeader(int header) {
        this.header = JLocale.getString(header);
    }

    public final void setInfoMessage(String text) {
        VirtualListItem par = createNewParser(false);
        par.addDescription(text, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        addPar(par);
    }

    private void addHeader() {
        if (null != header) {
            VirtualListItem line = createNewParser(false);
            line.addLabel(header,
                    Scheme.THEME_TEXT, Scheme.FONT_STYLE_BOLD);
            addPar(line);
            header = null;
        }
    }

    public void addParam(int langStr, String str) {
        addParam(JLocale.getString(langStr), str);
    }

    public void addParam(String langStr, String str) {
        if (!StringConvertor.isEmpty(str)) {
            addHeader();
            VirtualListItem line = createNewParser(true);
            line.addLabel(langStr + ": ",
                    Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
            line.addDescription(str, Scheme.THEME_PARAM_VALUE, Scheme.FONT_STYLE_PLAIN);
            addPar(line);
        }
    }

    public void addParamImage(int langStr, Icon img) {
        addParamImage(JLocale.getString(langStr), img);
    }

    public void addParamImage(String langStr, Icon img) {
        if (null != img) {
            addHeader();
            VirtualListItem line = createNewParser(true);
            if (!StringConvertor.isEmpty(langStr)) {
                line.addLabel(langStr + ": ",
                        Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
            }
            line.addImage(img.getImage());
            addPar(line);
        }
    }

    public void addAvatar(String langStr, Bitmap img) {
        if (null != img) {
            addHeader();
            VirtualListItem line = createNewParser(false);
            if (!StringConvertor.isEmpty(langStr)) {
                line.addLabel(langStr + ": ",
                        Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
            }
            line.addBitmap(img);
            addPar(line);
        }
    }

    public int getSize() {
        return elements.size();
    }

    public boolean isItemSelectable(int index) {
        if ((index < 0) || (getSize() <= index)) {
            return false;
        }
        return elements.get(index).isItemSelectable();
    }
}

