package ru.sawim.models;

import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 03.08.13
 * Time: 13:54
 * To change this template use File | Settings | File Templates.
 */
public class RosterItemView extends RelativeLayout {

    public TextView itemName;
    public TextView itemDescriptionText;
    public ImageView itemFirstImage;
    public ImageView itemSecondImage;
    public ImageView itemThirdImage;
    public ImageView itemFourthImage;
    public ImageView itemFifthImage;

    public RosterItemView(Context context) {
        super(context);
        setPadding(15, 15, 15, 15);
        itemFirstImage = new ImageView(context);
        itemFifthImage = new ImageView(context);
        itemSecondImage = new ImageView(context);
        itemThirdImage = new ImageView(context);
        itemName = new TextView(context);
        itemDescriptionText = new TextView(context);
        itemFourthImage = new ImageView(context);
        build();
    }

    private void build() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.alignWithParent = false;
        lp.addRule(CENTER_VERTICAL);
        itemFirstImage.setId(1);
        addView(itemFirstImage, lp);

        lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.alignWithParent = true;
        lp.addRule(CENTER_VERTICAL);
        lp.addRule(RIGHT_OF, itemFirstImage.getId());
        itemSecondImage.setPadding(6, 0, 0, 0);
        itemSecondImage.setId(2);
        addView(itemSecondImage, lp);

        lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.alignWithParent = true;
        lp.addRule(CENTER_VERTICAL);
        lp.addRule(RIGHT_OF, itemSecondImage.getId());
        itemThirdImage.setPadding(6, 0, 0, 0);
        itemThirdImage.setId(3);
        addView(itemThirdImage, lp);

        lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.alignWithParent = true;
        lp.addRule(RIGHT_OF, itemThirdImage.getId());
        lp.addRule(ALIGN_TOP, itemFifthImage.getId());
        lp.addRule(ALIGN_PARENT_TOP);
        itemName.setSingleLine(true);
        itemName.setPadding(5, 5, 5, 5);
        itemName.setId(4);
        addView(itemName, lp);

        lp = new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.alignWithParent = true;
        lp.addRule(RIGHT_OF, itemThirdImage.getId());
        lp.addRule(BELOW, itemName.getId());
        itemDescriptionText.setSingleLine(true);
        itemDescriptionText.setPadding(5, 0, 0, 0);
        itemDescriptionText.setId(5);
        addView(itemDescriptionText, lp);

        lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.alignWithParent = true;
        lp.addRule(ALIGN_PARENT_RIGHT);
        lp.addRule(ALIGN_BOTTOM, itemDescriptionText.getId());
        lp.addRule(ALIGN_TOP, itemName.getId());
        itemFifthImage.setId(6);
        addView(itemFifthImage, lp);

        lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.alignWithParent = true;
        lp.addRule(LEFT_OF, itemFifthImage.getId());
        lp.addRule(ALIGN_BOTTOM, itemDescriptionText.getId());
        lp.addRule(ALIGN_TOP, itemName.getId());
        itemFourthImage.setId(7);
        addView(itemFourthImage, lp);
    }
}
