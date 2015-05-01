package ru.sawim.view;

import android.view.Gravity;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import ru.sawim.activities.BaseActivity;
import ru.sawim.models.SmilesAdapter;
import ru.sawim.modules.Emotions;
import ru.sawim.roster.RosterHelper;
import ru.sawim.widget.SizeNotifierLinearLayout;
import ru.sawim.widget.Util;

public class SmileysPopup {

    private View smileysView;
    private PopupWindow popupWindow;
    private boolean keyboardVisible;

    public SmileysPopup(final BaseActivity activity, final SizeNotifierLinearLayout rootView) {
        rootView.setOnSizeChangedListener(new SizeNotifierLinearLayout.OnSizeChangedListener() {
            @Override
            public void onSizeChanged() {
                boolean oldValue = keyboardVisible;
                keyboardVisible = getPossibleKeyboardHeight(activity, rootView) > 100;
                if (keyboardVisible && keyboardVisible != oldValue) {
                    hide(rootView);
                } else if (!keyboardVisible && keyboardVisible != oldValue && isShown(rootView)) {
                    hide(rootView);
                }
            }
        });
    }

    public void show(BaseActivity activity, SizeNotifierLinearLayout rootView) {
        if (isShown(rootView)) {
            hide(rootView);
            return;
        }
        showView(activity, rootView);
    }

    private void showView(BaseActivity activity, final SizeNotifierLinearLayout rootView) {
        GridView grid = new GridView(activity);
        grid.setBackgroundResource(Util.getSystemBackground(activity));
        grid.setColumnWidth(Util.dipToPixels(activity, 60));
        grid.setNumColumns(-1);

        final SmilesAdapter smilesAdapter = new SmilesAdapter();
        grid.setAdapter(smilesAdapter);
        smilesAdapter.setOnItemClickListener(new SmilesAdapter.OnAdapterItemClickListener() {
            @Override
            public void onItemClick(SmilesAdapter adapterView, int position) {
                if (RosterHelper.getInstance().getUpdateChatListener() != null)
                    RosterHelper.getInstance().getUpdateChatListener().pastText(Emotions.instance.getSmileCode(position));
                hide(rootView);
            }
        });
        smileysView = grid;

        int keyboardHeight = getPossibleKeyboardHeight(activity, rootView);
        boolean showInsteadOfKeyboard = keyboardHeight > 100;
        if (showInsteadOfKeyboard) {
            if (popupWindow == null) {
                popupWindow = new PopupWindow(activity);
                popupWindow.setContentView(smileysView);
                popupWindow.setWidth(rootView.getWidth());
                popupWindow.setHeight(rootView.getHeight() / 2);
            }
            popupWindow.setHeight(keyboardHeight);
            popupWindow.showAtLocation(rootView, Gravity.BOTTOM, 0, 0);
            popupWindow.getContentView().setPadding(0, 0, 0, 0);
        } else {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            lp.weight = (float) 2.5;
            smileysView.setLayoutParams(lp);
            rootView.addView(smileysView);
        }
    }

    public boolean isShown(SizeNotifierLinearLayout rootView) {
        return (rootView.indexOfChild(smileysView) >= 0) || (popupWindow != null && popupWindow.isShowing());
    }

    public boolean hide(SizeNotifierLinearLayout rootView) {
        if (rootView.indexOfChild(smileysView) >= 0) {
            rootView.removeView(smileysView);
            return true;
        }
        if (popupWindow != null && popupWindow.isShowing()) {
            try {
                popupWindow.dismiss();
            } catch (Exception ignored) {
            }
            return true;
        }
        return false;
    }

    private int getPossibleKeyboardHeight(BaseActivity activity, SizeNotifierLinearLayout rootView) {
        int viewHeight = rootView.getHeight();
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

        return screenHeight - viewHeight - activity.getSupportActionBar().getHeight() - Util.getStatusBarHeight(activity);
    }
}
