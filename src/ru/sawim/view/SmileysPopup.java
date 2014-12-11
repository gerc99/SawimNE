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

    private final BaseActivity activity;
    SizeNotifierLinearLayout rootView;
    private View smileysView;

    private PopupWindow popupWindow;
    private boolean keyboardVisible;

    public SmileysPopup(BaseActivity activity, SizeNotifierLinearLayout view) {
        this.activity = activity;
        rootView = view;
        rootView.setOnSizeChangedListener(new SizeNotifierLinearLayout.OnSizeChangedListener() {
            @Override
            public void onSizeChanged() {
                boolean oldValue = keyboardVisible;
                keyboardVisible = getPossibleKeyboardHeight() > 100;
                if (keyboardVisible && keyboardVisible != oldValue) {
                    hide();
                } else if (!keyboardVisible && keyboardVisible != oldValue && isShown()) {
                    hide();
                }
            }
        });
    }

    public void show() {
        if (isShown()) {
            hide();
            return;
        }
        showView();
    }

    private void showView() {
        smileysView = getContentView();
        int keyboardHeight = getPossibleKeyboardHeight();
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

    public boolean isShown() {
        return (rootView.indexOfChild(smileysView) >= 0) || (popupWindow != null && popupWindow.isShowing());
    }

    public boolean hide() {
        if (rootView.indexOfChild(smileysView) >= 0) {
            rootView.removeView(smileysView);
            return true;
        }
        if (popupWindow != null && popupWindow.isShowing()) {
            try {
                popupWindow.dismiss();
            } catch (Exception e) {
            }
            return true;
        }
        return false;
    }

    private int getPossibleKeyboardHeight() {
        int viewHeight = rootView.getHeight();
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

        return screenHeight - viewHeight - activity.getSupportActionBar().getHeight() - Util.getStatusBarHeight(activity);
    }

    private View getContentView() {
        GridView grid = new GridView(activity);
        grid.setBackgroundResource(Util.getSystemBackground(activity));
        grid.setColumnWidth(Util.dipToPixels(activity, 60));
        grid.setNumColumns(-1);

        final SmilesAdapter smilesAdapter = new SmilesAdapter(activity);
        grid.setAdapter(smilesAdapter);
        smilesAdapter.setOnItemClickListener(new SmilesAdapter.OnAdapterItemClickListener() {
            @Override
            public void onItemClick(SmilesAdapter adapterView, int position) {
                if (RosterHelper.getInstance().getUpdateChatListener() != null)
                    RosterHelper.getInstance().getUpdateChatListener().pastText(Emotions.instance.getSmileCode(position));
                hide();
            }
        });
        return grid;
    }
}
