package ru.sawim.models.list;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
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
    private CharSequence label = null;
    private CharSequence descStr = null;
    private byte themeTextLabel = -1;
    private byte themeTextDesc = -1;
    private byte font;
    private boolean itemSelectable;
    private OnGroupListListener groupListListener;
    public boolean opened = false;
    public View itemView;

    public VirtualListItem(boolean itemSelectable) {
        this.itemSelectable = itemSelectable;
    }

    public void addLabel(CharSequence text, byte themeText, byte font) {
        label = text;
        this.themeTextLabel = themeText;
        this.font = font;
    }

    public void addLabel(int marginLeft, CharSequence text, byte themeText, byte font) {
        this.marginLeft = marginLeft;
        label = text;
        this.themeTextLabel = themeText;
        this.font = font;
    }

    public void addDescription(CharSequence text, byte themeText, byte font) {
        descStr = text;
        this.themeTextDesc = themeText;
        this.font = font;
    }

    public void addDescription(int marginLeft, CharSequence text, byte themeText, byte font) {
        this.marginLeft = marginLeft;
        descStr = text;
        this.themeTextDesc = themeText;
        this.font = font;
    }

    public OnGroupListListener getGroupListListener() {
        return groupListListener;
    }

    public void addGroup(int marginLeft, CharSequence text, byte themeText, byte font, OnGroupListListener groupListListener) {
        addImage(opened ? SawimResources.groupDownIcon : SawimResources.groupRightIcons);
        addDescription(marginLeft, text, themeText, font);
        this.groupListListener = groupListListener;
    }

    public CharSequence getLabel() {
        return label;
    }

    public CharSequence getDescStr() {
        if (descStr == null) return null;
        return TextFormatter.getInstance().parsedText(null, descStr);
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

    public interface OnGroupListListener {
        void select();
    }
}