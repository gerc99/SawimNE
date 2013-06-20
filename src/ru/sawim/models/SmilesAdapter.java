package ru.sawim.models;

import DrawControls.icons.AniIcon;
import DrawControls.icons.Icon;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import sawim.modules.Emotions;
import ru.sawim.General;
import ru.sawim.R;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 13.05.13
 * Time: 18:12
 * To change this template use File | Settings | File Templates.
 */
public class SmilesAdapter extends BaseAdapter {

    private Context baseContext;
    Emotions emotions;

    public SmilesAdapter(Context context) {
        baseContext = context;
        emotions = Emotions.instance;
    }

    @Override
    public int getCount() {
        return emotions.smiles().size();
    }

    @Override
    public Integer getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convView, ViewGroup viewGroup) {
        ItemWrapper wr;
        if (convView == null) {
            LayoutInflater inf = LayoutInflater.from(baseContext);
            convView = inf.inflate(R.layout.smiles_item, null);
            wr = new ItemWrapper(convView);
            convView.setTag(wr);
        } else {
            wr = (ItemWrapper) convView.getTag();
        }
        wr.populateFrom(getItem(i));
        return convView;
    }

    private final static int DURATION = 100;
    private AnimationDrawable mAnimation = null;

    private void startFrameAnimation(ImageView mImage, Bitmap bm) {
        BitmapDrawable frame = new BitmapDrawable(baseContext.getResources(), bm);
        mAnimation = new AnimationDrawable();
        mAnimation.setOneShot(false);
        mAnimation.addFrame(frame, DURATION);
        mImage.setBackgroundDrawable(mAnimation);

    }

    private void stopFrameAnimation() {
        if (mAnimation.isRunning()) {
            mAnimation.stop();
            mAnimation.setVisible(false, false);
        }
    }


    public class ItemWrapper {
        View item = null;
        private ImageView itemImage = null;

        public ItemWrapper(View item) {
            this.item = item;
        }

        /*void populateFrom(int item) {
            AniIcon ic = (AniIcon) emotions.smiles().iconAt(item);
            if (ic != null) {
                //getItemImage().setImageBitmap(iconToBitmap(ic));
                Log.e("frameCount", "" + ic.getImages().length);
                for (int frameNum = 0; frameNum < ic.getImages().length; ++frameNum) {
                    startFrameAnimation(getItemImage(), General.iconToBitmap(ic.getImages()[frameNum]));
                }
                if (!mAnimation.isRunning()) {
                    mAnimation.setVisible(true, true);
                    mAnimation.start();
                }
            }
        }*/

        void populateFrom(int item) {
            Icon ic = emotions.smiles().iconAt(item);
            if (ic != null) {
                getItemImage().setImageBitmap(General.iconToBitmap(ic));
            }
        }

        public ImageView getItemImage() {
            if (itemImage == null) {
                itemImage = (ImageView) item.findViewById(R.id.smileImage);
            }
            return itemImage;
        }
    }
}