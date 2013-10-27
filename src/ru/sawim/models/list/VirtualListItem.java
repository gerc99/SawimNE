package ru.sawim.models.list;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import ru.sawim.General;
import ru.sawim.SawimApplication;
import ru.sawim.text.TextFormatter;

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
    private SpannableStringBuilder descSpan = null;
    private byte themeTextLabel = -1;
    private byte themeTextDesc = -1;
    private byte font;
    private boolean itemSelectable;
    private boolean hasLinks;
    private OnGroupListListener groupListListener;
    public boolean opened = false;

    public VirtualListItem(boolean itemSelectable) {
        this.itemSelectable = itemSelectable;
    }

    public void addLabel(String text, byte themeText, byte font) {
        label = text;
        this.themeTextLabel = themeText;
        this.font = font;
    }

    public void addLabel(int marginLeft, String text, byte themeText, byte font) {
        this.marginLeft = marginLeft;
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

    public OnGroupListListener getGroupListListener() {
        return groupListListener;
    }

    public void addGroup(int marginLeft, String text, byte themeText, byte font, OnGroupListListener groupListListener) {
        addImage(opened ? General.groupDownIcon.iconAt(0).getImage() : General.groupRightIcons.iconAt(0).getImage());
        addDescription(marginLeft, text, themeText, font);
        this.groupListListener = groupListListener;
    }

    public void addTextWithSmiles(String text, byte themeText, byte font) {
        descSpan = new SpannableStringBuilder(text);
        TextFormatter.detectEmotions(descSpan, text);
        //TextFormatter.getInstance().getTextWithLinks(descSpan, 0xff00e4ff, -1);
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

    public interface OnGroupListListener {
        void select();
    }
}