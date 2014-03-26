package ru.sawim.view;

import android.app.Activity;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.PopupWindow;
import ru.sawim.models.SmilesAdapter;
import ru.sawim.modules.Emotions;
import ru.sawim.roster.RosterHelper;
import ru.sawim.widget.SizeNotifierLinearLayout;
import ru.sawim.widget.Util;

public class SmileysPopup {

    private final Activity activity;
    SizeNotifierLinearLayout rootView;

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
        View smileysView = getContentView();
        if (popupWindow == null) {
            popupWindow = new PopupWindow(activity);
            popupWindow.setContentView(smileysView);
            popupWindow.setWidth(activity.getWindow().getDecorView().getWidth());
            popupWindow.setHeight(activity.getWindow().getDecorView().getHeight() / 2);
        }

        Rect rect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);

        boolean showInsteadOfKeyboard = getPossibleKeyboardHeight() > 100;
        if (showInsteadOfKeyboard) {
            popupWindow.setHeight(getPossibleKeyboardHeight());
            popupWindow.showAtLocation(activity.getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);
            popupWindow.getContentView().setPadding(0, 0, 0, 0);
        } else {
            int keyBoardHeight = (rect.height() - this.editText.getHeight()) / 2;
            popupWindow.setHeight(keyBoardHeight);
            popupWindow.showAtLocation(activity.getWindow().getDecorView(), Gravity.TOP, rect.left, rect.top + keyBoardHeight);
        }
    }

    public boolean isShown() {
        return popupWindow != null && popupWindow.isShowing();
    }

    public boolean hide() {
        if (isShown()) {
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
        grid.setColumnWidth(Util.dipToPixels(activity, 45));
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
