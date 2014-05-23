package ru.sawim.view;

import android.app.Activity;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import ru.sawim.models.SmilesAdapter;
import ru.sawim.modules.Emotions;
import ru.sawim.roster.RosterHelper;
import ru.sawim.widget.SizeNotifierLinearLayout;
import ru.sawim.widget.Util;

public class SmileysPopup {

    private final Activity activity;
    SizeNotifierLinearLayout rootView;
    private View smileysView;

    private PopupWindow popupWindow;
    private EditText editText;
    private boolean keyboardVisible;

    public SmileysPopup(Activity activity, SizeNotifierLinearLayout view, EditText editText) {
        this.activity = activity;
        rootView = view;
        this.editText = editText;
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
            Rect rect = new Rect();
            rootView.getWindowVisibleDisplayFrame(rect);
            int keyBoardHeight = (rect.height() - this.editText.getHeight()) / 2;
            //popupWindow.setHeight(keyBoardHeight);
            //popupWindow.showAtLocation(rootView, Gravity.TOP, rect.left, rect.top + keyBoardHeight);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            lp.height = keyBoardHeight;
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
        int statusBarHeight = activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();

        return screenHeight - viewHeight - statusBarHeight;
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
