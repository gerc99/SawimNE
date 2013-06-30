package ru.sawim.models.list;

import DrawControls.icons.Icon;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Bitmap;
import ru.sawim.models.form.VirtualListItem;
import sawim.comm.StringConvertor;
import ru.sawim.Scheme;
import sawim.util.JLocale;


public final class VirtualListModel {
    public List<VirtualListItem> elements;
    private String header = null;
    private OnAddListListener addListListener;

    public VirtualListModel() {
        elements = new ArrayList<VirtualListItem>();
    }

    public final void addPar(final VirtualListItem item) {
        if (addListListener == null)
            elements.add(item);
        else
            addListListener.addList(item);
    }

    public void clear() {
        if (addListListener == null)
            elements.clear();
        else
            addListListener.clearList();
        header = null;
    }

    public void removeFirstText() {
        if (addListListener == null)
            elements.remove(0);
        else
            addListListener.removeFirstText();
    }

    public void setAddListListener(OnAddListListener addListListener) {
        this.addListListener = addListListener;
    }

    public interface OnAddListListener {
        void addList(VirtualListItem item);
        void clearList();
        void removeFirstText();
    }

    public final VirtualListItem createNewParser(boolean itemSelectable) {
        return new VirtualListItem(itemSelectable);
    }
    
    public final void addItem(String text, boolean active) {
        byte type = active ?  Scheme.FONT_STYLE_BOLD :  Scheme.FONT_STYLE_PLAIN;
        VirtualListItem item = createNewParser(true);
        item.addDescription(text, Scheme.THEME_TEXT, type);
        addPar(item);
    }
    
    public final void setHeader(String header) {
        this.header = header;
    }

    public final void setInfoMessage(String text) {
        VirtualListItem par = createNewParser(false);
        par.addDescription(text, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        addPar(par);
    }

    private void addHeader() {
        if (null != header) {
            VirtualListItem line = createNewParser(false);
            line.addLabel(JLocale.getString(header),
                    Scheme.THEME_TEXT, Scheme.FONT_STYLE_BOLD);
            addPar(line);
            header = null;
        }
    }

    public void addParam(String langStr, String str) {
        if (!StringConvertor.isEmpty(str)) {
            addHeader();
            VirtualListItem line = createNewParser(true);
            line.addLabel(JLocale.getString(langStr) + ": ",
                    Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
            line.addDescription(str, Scheme.THEME_PARAM_VALUE, Scheme.FONT_STYLE_PLAIN);
            addPar(line);
        }
    }

    public void addParamImage(String langStr, Icon img) {
        if (null != img) {
            addHeader();
            VirtualListItem line = createNewParser(true);
            if (!StringConvertor.isEmpty(langStr)) {
                line.addLabel(JLocale.getString(langStr) + ": ",
                        Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
            }
            line.addIcon(img);
            addPar(line);
        }
    }
    public void addAvatar(String langStr, Bitmap img) {
        if (null != img) {
            addHeader();
            VirtualListItem line = createNewParser(false);
            if (!StringConvertor.isEmpty(langStr)) {
                line.addLabel(JLocale.getString(langStr) + ": ",
                        Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
            }
            line.addBitmapImage(img);
            addPar(line);
        }
    }

    public int getSize() {
        return elements.size();
    }

    public boolean isItemSelectable(int i) {
        //if (elements.size() < i) return false;
        return elements.get(i).isItemSelectable();
    }
}

