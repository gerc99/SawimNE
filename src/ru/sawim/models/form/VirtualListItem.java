package ru.sawim.models.form;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import ru.sawim.SawimApplication;
import ru.sawim.activities.SawimActivity;
import sawim.TextFormatter;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.06.13
 * Time: 12:17
 * To change this template use File | Settings | File Templates.
 */
public class VirtualListItem {
    private int marginLeft = 0;
    private Drawable image;
    private String label = null;
    private String descStr = null;
    private Spannable descSpan = null;
    private byte themeTextLabel = -1;
    private byte themeTextDesc = -1;
    private byte font;
    private boolean itemSelectable;
    private boolean hasLinks;

    public VirtualListItem(boolean itemSelectable) {
        this.itemSelectable = itemSelectable;
    }

    public void addLabel(String text, byte themeText, byte font) {
        label = text;
        this.themeTextLabel = themeText;
        this.font = font;
    }

    public void addDescription(String text, byte themeText, byte font) {
        descStr = text;
        this.themeTextDesc = themeText;
        this.font = font;
    }

    public void addDescription(int marginLeft, String text, byte themeText, byte font) {
        this.marginLeft = marginLeft;
        descStr = text;
        this.themeTextDesc = themeText;
        this.font = font;
    }

    public void addTextWithSmiles(String text, byte themeText, byte font) {
        descSpan = TextFormatter.getFormattedText(text, SawimActivity.getInstance());
        this.themeTextDesc = themeText;
        this.font = font;
    }

    public String getLabel() {
        return label;
    }

    public String getDescStr() {
        return descStr;
    }

    public Spannable getDescSpan() {
        return descSpan;
    }

    public byte getThemeTextLabel() {
        return themeTextLabel;
    }

    public byte getThemeTextDesc() {
        return themeTextDesc;
    }

    public byte getFont() {
        return font;
    }

    public void addImage(Drawable ic) {
        image = ic;
    }

    public void addBitmap(Bitmap b) {
        image = new BitmapDrawable(SawimApplication.getContext().getResources(), b);
    }

    public Drawable getImage() {
        return image;
    }

    public void addBr() {
        descStr = "\n";
    }

    public int getMarginLeft() {
        return marginLeft;
    }

    public boolean isItemSelectable() {
        return itemSelectable;
    }


    public void setHasLinks(boolean hasLinks) {
        this.hasLinks = hasLinks;
    }

    public boolean isHasLinks() {
        return hasLinks;
    }
}