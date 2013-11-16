package ru.sawim.models;

import DrawControls.icons.AniIcon;
import DrawControls.icons.Icon;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import ru.sawim.R;
import sawim.modules.Emotions;

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
        return emotions.count();
    }

    @Override
    public Icon getItem(int i) {
        return emotions.getSmile(i);
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
        if (emotions.isAniSmiles())
            wr.populateAniFrom((AniIcon) getItem(i));
        else
            wr.populateFrom(getItem(i));
        return convView;
    }

    private AnimationDrawable mAnimation = null;

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
            itemImage = (ImageView) item.findViewById(R.id.smileImage);
        }

        void populateAniFrom(AniIcon ic) {
            mAnimation = new AnimationDrawable();
            if (ic != null) {
                mAnimation.setOneShot(false);
                for (int frameNum = 0; frameNum < ic.getImages().length; ++frameNum) {
                    mAnimation.addFrame(ic.getImages()[frameNum].getImage(), ic.getDelays()[frameNum]);
                }
                itemImage.setImageDrawable(mAnimation);
            }
            if (!mAnimation.isRunning()) {
                mAnimation.setVisible(true, true);
                mAnimation.start();
            }
        }

        void populateFrom(Icon ic) {
            if (ic != null) {
                itemImage.setImageDrawable(ic.getImage());
            }
        }
    }
}