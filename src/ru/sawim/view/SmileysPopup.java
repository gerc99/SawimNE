package ru.sawim.view;

import android.app.Activity;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.PopupWindow;
import ru.sawim.models.SmilesAdapter;
import ru.sawim.widget.Util;
import sawim.modules.Emotions;
import sawim.roster.RosterHelper;

public class SmileysPopup {

	private final Activity activity;
    View rootView;

	private PopupWindow popupWindow;
	private EditText editText;

	public SmileysPopup(Activity activity, View view) {
		this.activity = activity;
        rootView = view;
	}

	public void show(EditText editText) {
        if (isShown()) {
            hide();
            return;
        }
        View smileysView = getContentView();
		this.editText = editText;
		if (popupWindow == null) {
			popupWindow = new PopupWindow(activity);
			popupWindow.setContentView(smileysView);
			popupWindow.setWidth(activity.getWindow().getDecorView().getWidth());
			popupWindow.setHeight(activity.getWindow().getDecorView().getHeight() / 2);
		}

        Rect rect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);

        boolean showInsteadOfKeyboard = showInsteadOfKeyboard();
        if (showInsteadOfKeyboard) {
            popupWindow.setHeight(getPossibleKeyboardHeight());
            popupWindow.showAtLocation(activity.getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);
            popupWindow.getContentView().setPadding(0, 0, 0, 0);
        } else {
            int keyBoardHeight = rect.height() - this.editText.getHeight();
            popupWindow.setHeight(keyBoardHeight);
            popupWindow.showAtLocation(activity.getWindow().getDecorView(), Gravity.TOP, rect.left, rect.top);
        }
	}

    public boolean isShown() {
        return popupWindow != null && popupWindow.isShowing();
    }

    public void hide() {
        if (isShown()) {
            try {
                popupWindow.dismiss();
            } catch (Exception e) {
            }
        }
    }

	private boolean showInsteadOfKeyboard() {
		return getPossibleKeyboardHeight() > 100;
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
        grid.setNumColumns(5);

        SmilesAdapter smilesAdapter = new SmilesAdapter(activity);
        grid.setAdapter(smilesAdapter);
        smilesAdapter.setOnItemClickListener(new SmilesAdapter.OnAdapterItemClickListener() {
            @Override
            public void onItemClick(SmilesAdapter adapterView, int position) {
                if (RosterHelper.getInstance().getUpdateChatListener() != null)
                    RosterHelper.getInstance().getUpdateChatListener().pastText(Emotions.instance.getSmileCode(position));
            }
        });
		return grid;
	}
}
