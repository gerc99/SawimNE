package ru.sawim.models.form;

import DrawControls.icons.Icon;
import DrawControls.icons.Image;
import android.graphics.Bitmap;
import android.text.Spannable;
import ru.sawim.General;
import ru.sawim.activities.VirtualListActivity;
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
    private Bitmap image;
    private String label = null;
    private String descStr = null;
    private Spannable descSpan = null;
    private byte themeTextLabel = -1;
    private byte themeTextDesc = -1;
    private byte font;
    private boolean textIsSelectable = false;
    private boolean itemSelectable;

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

    public void addDescriptionSelectable(String text, byte themeText, byte font) {
        textIsSelectable = true;
        descStr = text;
        this.themeTextDesc = themeText;
        this.font = font;
    }

    public void addTextWithSmiles(String text, byte themeText, byte font) {
        descSpan = TextFormatter.getFormattedText(VirtualListActivity.getInstance(), text, General.getColor(themeText));
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

    public void addBitmapImage(Bitmap b) {
        image = b;
    }

    public void addIcon(Icon ic) {
        image = General.iconToBitmap(ic);
    }

    public void addImage(Image ic) {
        image = General.imageToBitmap(ic);
    }

    public Bitmap getImage() {
        return image;
    }

    public boolean isTextSelectable() {
        return textIsSelectable;
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
}
