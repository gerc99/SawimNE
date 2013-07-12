package ru.sawim.models.form;

import DrawControls.icons.Image;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import ru.sawim.General;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.modules.DebugLog;
import sawim.util.JLocale;
import ru.sawim.activities.FormActivity;
import ru.sawim.activities.SawimActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 02.06.13
 * Time: 22:04
 * To change this template use File | Settings | File Templates.
 */
public class Forms {

    public List<Control> controls = new ArrayList<Control>();
    public static final byte CONTROL_TEXT = 0;
    public static final byte CONTROL_INPUT = 1;
    public static final byte CONTROL_CHECKBOX = 2;
    public static final byte CONTROL_SELECT = 3;
    public static final byte CONTROL_GAUGE = 4;
    public static final byte CONTROL_IMAGE = 5;
    public static final byte CONTROL_LINK = 6;
    private OnUpdateForm updateFormListener;
    private FormListener formListener;
    private OnBackPressed backPressedListener;
    private ControlStateListener controlListener;
    public CharSequence caption;
    private static Forms instance;

    public void back() {
        updateFormListener.back();
        destroy();
    }

    public void backForm() {
        updateFormListener.back();
    }

    public void show() {
        SawimActivity.getInstance().startActivity(new Intent(SawimActivity.getInstance(), FormActivity.class));
    }

    public void show(FragmentActivity a) {
        a.startActivity(new Intent(a, FormActivity.class));
    }

    public void invalidate() {
        if (updateFormListener != null)
            updateFormListener.updateForm();
    }

    public void setUpdateFormListener(OnUpdateForm l) {
        updateFormListener = l;
    }

    public FormListener getFormListener() {
        return formListener;
    }

    public static Forms getInstance() {
        return instance;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public interface OnUpdateForm {
        void updateForm();
        void back();
    }

    public interface OnBackPressed {
        boolean back();
    }

    public OnBackPressed getBackPressedListener() {
        return backPressedListener;
    }

    public void setBackPressedListener(OnBackPressed backPressedListener) {
        this.backPressedListener = backPressedListener;
    }

    public static class Control {
        public int id;
        public String description;
        public String label;
        public byte type;

        public String text; // input
        public boolean selected;// checkbox
        public String[] items;// select
        public int current;
        public int level;// gauge
        public Bitmap image;
    }

    public Forms(String caption_, FormListener l) {
        caption = JLocale.getString(caption_);
        formListener = l;
		instance = this;
    }

    public void setControlStateListener(ControlStateListener l) {
        controlListener = l;
    }

    public void destroy() {
        clearForm();
        clearListeners();
    }

    public void clearListeners() {
        formListener = null;
        controlListener = null;
        updateFormListener = null;
        backPressedListener = null;
    }

    private Control create(int controlId, byte type, String label, String desc) {
        Control c = new Control();
        c.id = controlId;
        c.type = type;
        if ((null != label) || (null != desc)) {
            if (null != label) {
                c.label = label;
            }
            if (null != desc) {
                c.description = desc;
            }
        }
        return c;
    }

    private void add(Control c) {
        controls.add(c);
        invalidate();
    }

    public void clearForm() {
        controls.clear();
    }

    public void addSelector(int controlId, String label, String items, int index) {
        String[] all = Util.explode(items, '|');
        for (int i = 0; i < all.length; ++i) {
            all[i] = JLocale.getString(all[i]);
        }
        addSelector(controlId, label, all, index);
    }

    public void addSelector(int controlId, String label, String[] items, int index) {
        label = (null == label) ? " " : label;
        Control c = create(controlId, CONTROL_SELECT, null, JLocale.getString(label));
        c.items = items;
        c.current = index % c.items.length;
        add(c);
    }

    public void addVolumeControl(int controlId, String label, int current) {
        label = (null == label) ? " " : label;
        Control c = create(controlId, CONTROL_GAUGE, null, JLocale.getString(label));
        c.level = current / 10;
        add(c);
    }

    public void addCheckBox(int controlId, String label, boolean selected) {
        label = (null == label) ? " " : label;
        Control c = create(controlId, CONTROL_CHECKBOX, null, JLocale.getString(label));
        c.selected = selected;
        add(c);
    }

    public void addCheckBox_(int controlId, String label, boolean selected) {
        label = (null == label) ? " " : label;
        Control c = create(controlId, CONTROL_CHECKBOX, null, label);
        c.selected = selected;
        add(c);
    }

    public void addHeader(String label) {
        add(create(-1, CONTROL_TEXT, JLocale.getString(label), null));
    }

    public void addString(String label, String text) {
        add(create(-1, CONTROL_TEXT, JLocale.getString(label), text));
    }

    public void addString(String text) {
        addString(null, text);
    }

    public void addString(int controlId, String text) {
        add(create(controlId, CONTROL_TEXT, null, text));
    }

    public void addLink(int controlId, String text) {
        add(create(controlId, CONTROL_LINK, null, text));
    }

    public void addTextField(int controlId, String label, String text) {
        addTextField_(controlId, label, text);
    }

    public void addPasswordField(int controlId, String label, String text) {
        addTextField_(controlId, label, text);
    }

    public void addTextField_(int controlId, String label, String text) {
        label = (null == label) ? " " : label;
        Control c = create(controlId, CONTROL_INPUT, null, JLocale.getString(label));
        text = StringConvertor.notNull(text);
        c.text = text;
        add(c);
    }

    public void addImage(Image img) {
        Control c = create(-1, CONTROL_IMAGE, null, null);
        c.image = img.getBitmap();
        add(c);
    }

    public void addBitmap(Bitmap img) {
        Control c = create(-1, CONTROL_IMAGE, null, null);
        c.image = img;
        add(c);
    }

    public void remove(int controlId) {
        try {
            for (int num = 0; num < controls.size(); ++num) {
                if ((controls.get(num)).id == controlId) {
                    controls.remove(num);
                    invalidate();
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public Control get(int controlId) {
        for (int num = 0; num < controls.size(); ++num) {
            if ((controls.get(num)).id == controlId) {
                return controls.get(num);
            }
        }
        return null;
    }
    public boolean hasControl(int controlId) {
        return null != get(controlId);
    }
    public void setTextFieldLabel(int controlId, String desc) {
        Control c = get(controlId);
        c.description = desc;
        invalidate();
    }

    public int getGaugeValue(int controlId) {
        try {
            return get(controlId).level;
        } catch (Exception e) {
            DebugLog.panic("getGaugeValue", e);
        }
        return 0;
    }
    public int getVolumeValue(int controlId) {
        return getGaugeValue(controlId) * 10;
    }
    public String getTextFieldValue(int controlId) {
        try {
            return get(controlId).text;
        } catch (Exception e) {
            DebugLog.panic("getTextFieldValue", e);
        }
        return null;
    }
    public int getSelectorValue(int controlId) {
        try {
            Log.e("Forms", ""+controlId);
            return get(controlId).current;
        } catch (Exception e) {
            DebugLog.panic("getSelectorValue", e);
        }
        return 0;
    }
    public String getSelectorString(int controlId) {
        try {
            return get(controlId).items[get(controlId).current];
        } catch (Exception e) {
            DebugLog.panic("getSelectorString", e);
        }
        return null;
    }

    public boolean getCheckBoxValue(int controlId) {
        try {
            return get(controlId).selected;
        } catch (Exception e) {
            DebugLog.panic("getChoiceItemValue", e);
        }
        return false;
    }

    public int getSize() {
        return controls.size();
    }

    public void controlUpdated(Control c) {
        if (null != controlListener) {
            controlListener.controlStateChanged(c.id);
        }
    }
}
