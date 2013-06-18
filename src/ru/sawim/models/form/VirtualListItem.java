package ru.sawim.models.form;

import DrawControls.icons.Icon;
import DrawControls.icons.Image;
import android.graphics.Bitmap;
import android.widget.ImageView;
import ru.sawim.General;

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
    private String description = null;
    private byte themeText;
    private byte font;
    private boolean textIsSelectable = false;
    private boolean itemSelectable;

    public VirtualListItem(boolean itemSelectable) {
        this.itemSelectable = itemSelectable;
    }

    public void addLabel(String text, byte themeText, byte font) {
        label = text;
        this.themeText = themeText;
        this.font = font;
    }

    public void addDescription(String text, byte themeText, byte font) {
        description = text;
        this.themeText = themeText;
        this.font = font;
    }

    public void addDescription(int marginLeft, String text, byte themeText, byte font) {
        this.marginLeft = marginLeft;
        description = text;
        this.themeText = themeText;
        this.font = font;
    }

    public void addDescriptionSelectable(String text, byte themeText, byte font) {
        textIsSelectable = true;
        description = text;
        this.themeText = themeText;
        this.font = font;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public byte getThemeText() {
        return themeText;
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
        description = "\n";
    }

    public int getMarginLeft() {
        return marginLeft;
    }

    public boolean isItemSelectable() {
        return itemSelectable;
    }
}
