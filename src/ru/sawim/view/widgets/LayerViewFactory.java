package ru.sawim.view.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import ru.sawim.General;
import ru.sawim.Scheme;
import ru.sawim.R;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 20.05.13
 * Time: 22:33
 * To change this template use File | Settings | File Templates.
 */
public class LayerViewFactory {
    private Context baseContext;

    public LayerViewFactory(Context context) {
        baseContext = context;
    }

    public View getView(View convView, String layer) {
        ItemWrapper wr;
        if (convView == null) {
            LayoutInflater inf = LayoutInflater.from(baseContext);
            convView = inf.inflate(R.layout.muc_users_item, null);
            wr = new ItemWrapper(convView);
            convView.setTag(wr);
        } else {
            wr = (ItemWrapper) convView.getTag();
        }
        wr.populateFrom(layer);
        return convView;
    }

    public class ItemWrapper {
        View item = null;
        private TextView itemLayer = null;

        public ItemWrapper(View item) {
            this.item = item;
        }

        void populateFrom(String layer) {
            TextView itemLayer = getItemLayer();
			itemLayer.setTextSize(General.getFontSize() - 2);
            itemLayer.setTypeface(Typeface.SANS_SERIF);
            itemLayer.setText(layer);
            itemLayer.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        }

        public TextView getItemLayer() {
            if (itemLayer == null) {
                itemLayer = (TextView) item.findViewById(R.id.item_name);
            }
            return itemLayer;
        }
    }
}
