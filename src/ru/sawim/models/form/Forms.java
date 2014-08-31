package ru.sawim.models.form;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import ru.sawim.SawimApplication;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.DebugLog;
import ru.sawim.view.FormView;

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
    public static final byte CONTROL_GAUGE_FONT = 7;
    public static final byte CONTROL_BUTTON = 8;
    public static final byte CONTROL_RINGTONE = 9;
    private OnUpdateForm updateFormListener;
    private FormListener formListener;
    private ControlStateListener controlListener;
    private String caption;
    private boolean isAccept;
    private String waitingString;
    private String warningString;

    public static class Control {
        public String id;
        public String description;
        public String label;
        public byte type;
        public boolean isActive = true;

        public String text; // input
        public boolean selected;// checkbox
        public String[] items;// select
        public int current;
        public int level;// gauge
        public Drawable image;
    }

    public Forms(int caption_, FormListener l, boolean isAccept) {
        this(JLocale.getString(caption_), l, isAccept);
    }

    public Forms(String caption_, FormListener l, boolean isAccept) {
        caption = caption_;
        formListener = l;
        this.isAccept = isAccept;
    }

    public String getWarningString() {
        return warningString;
    }

    public void setWarningString(String warningString) {
        this.warningString = warningString;
    }

    public String getWaitingString() {
        return waitingString;
    }

    public void setWaitingString(String waitingString) {
        this.waitingString = waitingString;
    }

    public void setControlStateListener(ControlStateListener l) {
        controlListener = l;
    }

    public void setUpdateFormListener(OnUpdateForm l) {
        updateFormListener = l;
    }

    public FormListener getFormListener() {
        return formListener;
    }

    public boolean isAccept() {
        return isAccept;
    }

    public void setCaption(String text) {
        caption = text;
    }

    public String getCaption() {
        return caption;
    }

    public interface OnUpdateForm {
        void updateForm(boolean isLoad);

        void back();
    }

    public void show(BaseActivity activity) {
        FormView.show(activity, this);
    }

    public void showCaptcha() {
        FormView.showCaptcha(this);
    }

    public void invalidate(boolean isLoad) {
        if (updateFormListener != null)
            updateFormListener.updateForm(isLoad);
    }

    public void back() {
        if (updateFormListener != null)
            updateFormListener.back();
    }

    private Control create(String controlId, byte type, String label, String desc) {
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
    }

    public int getSize() {
        return controls.size();
    }

    public void clearForm() {
        controls.clear();
    }

    public void addSelector(String controlId, int label, String items, int index) {
        String[] all = Util.explode(items, '|');
        for (int i = 0; i < all.length; ++i) {
            all[i] = all[i];
        }
        addSelector(controlId, label, all, index);
    }

    public void addSelector(int controlId, int label, String items, int index) {
        addSelector(String.valueOf(controlId), label, items, index);
    }

    public void addSelector(String controlId, String label, String items, int index) {
        String[] all = Util.explode(items, '|');
        for (int i = 0; i < all.length; ++i) {
            all[i] = all[i];
        }
        Control c = create(controlId, CONTROL_SELECT, null, label);
        c.items = all;
        c.current = index % c.items.length;
        add(c);
    }

    public void addSelector(int controlId, String label, String items, int index) {
        addSelector(String.valueOf(controlId), label, items, index);
    }

    public void addSelector(String controlId, int label, int[] items, int index) {
        String[] all = new String[items.length];
        for (int i = 0; i < all.length; ++i) {
            all[i] = JLocale.getString(items[i]);
        }
        addSelector(controlId, label, all, index);
    }

    public void addSelector(int controlId, int label, int[] items, int index) {
        addSelector(String.valueOf(controlId), label, items, index);
    }

    public void addSelector(String controlId, int label, String[] items, int index) {
        Control c = create(controlId, CONTROL_SELECT, null, JLocale.getString(label));
        c.items = items;
        c.current = index % c.items.length;
        add(c);
    }

    public void addSelector(int controlId, int label, String[] items, int index) {
        addSelector(String.valueOf(controlId), label, items, index);
    }

    public void addVolumeControl_(String controlId, int label, int current) {
        Control c = create(controlId, CONTROL_GAUGE, null, JLocale.getString(label));
        c.level = current / 10;
        add(c);
    }

    public void addFontVolumeControl(String controlId, int label, int current) {
        Control c = create(controlId, CONTROL_GAUGE_FONT, null, JLocale.getString(label));
        c.level = current;
        add(c);
    }

    public void addCheckBox(String controlId, int label, boolean selected) {
        Control c = create(controlId, CONTROL_CHECKBOX, null, JLocale.getString(label));
        c.selected = selected;
        add(c);
    }

    public void addCheckBox(int controlId, int label, boolean selected) {
        addCheckBox(String.valueOf(controlId), label, selected);
    }

    public void addCheckBox(String controlId, String label, boolean selected) {
        label = (null == label) ? " " : label;
        Control c = create(controlId, CONTROL_CHECKBOX, null, label);
        c.selected = selected;
        add(c);
    }

    public void addCheckBox(int controlId, String label, boolean selected) {
        addCheckBox(String.valueOf(controlId), label, selected);
    }

    public void addHeader(int label) {
        add(create("", CONTROL_TEXT, JLocale.getString(label), null));
    }

    public void addString(int label, String text) {
        add(create("", CONTROL_TEXT, JLocale.getString(label), text));
    }

    public void addString(String label, String text) {
        add(create("", CONTROL_TEXT, label, text));
    }

    public void addString(String text) {
        addString(text, null);
    }

    public void addString_(String controlId, String text) {
        add(create(controlId, CONTROL_TEXT, null, text));
    }

    public void addString_(int controlId, String text) {
        add(create(String.valueOf(controlId), CONTROL_TEXT, null, text));
    }

    public void addLink(String controlId, String text) {
        add(create(controlId, CONTROL_LINK, null, text));
    }

    public void addButton(String controlId, String text) {
        add(create(controlId, CONTROL_BUTTON, null, text));
    }

    public void addButton(int controlId, String text) {
        add(create(String.valueOf(controlId), CONTROL_BUTTON, null, text));
    }

    public void addRingtoneControl(String controlId, String label, String desc) {
        add(create(controlId, CONTROL_RINGTONE, label, desc));
    }

    public void addTextField(String controlId, int label, String text) {
        addTextField_(controlId, label, text);
    }

    public void addTextField(int controlId, int label, String text) {
        addTextField_(String.valueOf(controlId), label, text);
    }

    public void addTextField(String controlId, String label, String text) {
        addTextField_(controlId, label, text);
    }

    public void addTextField(int controlId, String label, String text) {
        addTextField_(String.valueOf(controlId), label, text);
    }

    public void addPasswordField(String controlId, int label, String text) {
        addTextField_(controlId, label, text);
    }

    public void addPasswordField(String controlId, String label, String text) {
        addTextField_(controlId, label, text);
    }

    public void addPasswordField(int controlId, String label, String text) {
        addTextField_(String.valueOf(controlId), label, text);
    }

    private void addTextField_(String controlId, int label, String text) {
        addTextField_(controlId, JLocale.getString(label), text);
    }

    private void addTextField_(String controlId, String label, String text) {
        Control c = create(controlId, CONTROL_INPUT, null, label);
        text = StringConvertor.notNull(text);
        c.text = text;
        add(c);
    }

    public void addDrawable(Drawable img) {
        Control c = create("", CONTROL_IMAGE, null, null);
        c.image = img;
        add(c);
    }

    public void addBitmap(Bitmap img) {
        Control c = create("", CONTROL_IMAGE, null, null);
        c.image = new BitmapDrawable(SawimApplication.getContext().getResources(), img);
        add(c);
    }

    public void remove(String controlId) {
        try {
            for (int num = 0; num < controls.size(); ++num) {
                if ((controls.get(num)).id.equals(controlId)) {
                    controls.remove(num);
                    invalidate(true);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void remove(int controlId) {
        remove(String.valueOf(controlId));
    }

    public Control get(String controlId) {
        for (int num = 0; num < controls.size(); ++num) {
            if ((controls.get(num)).id.equals(controlId)) {
                return controls.get(num);
            }
        }
        return null;
    }

    public boolean hasControl(String controlId) {
        return null != get(controlId);
    }

    public boolean hasControl(int controlId) {
        return null != get(String.valueOf(controlId));
    }

    public void setTextFieldLabel(String controlId, String desc) {
        Control c = get(controlId);
        c.description = desc;
    }

    public int getGaugeValue(String controlId) {
        try {
            return get(controlId).level;
        } catch (Exception e) {
            DebugLog.panic("getGaugeValue", e);
        }
        return 0;
    }

    public int getVolumeValue(String controlId) {
        return getGaugeValue(controlId) * 10;
    }

    public String getTextFieldValue(String controlId) {
        try {
            return get(controlId).text;
        } catch (Exception e) {
            DebugLog.panic("getTextFieldValue", e);
        }
        return null;
    }

    public String getTextFieldValue(int controlId) {
        return getTextFieldValue(String.valueOf(controlId));
    }

    public int getSelectorValue(String controlId) {
        try {
            return get(controlId).current;
        } catch (Exception e) {
            DebugLog.panic("getSelectorValue", e);
        }
        return 0;
    }

    public int getSelectorValue(int controlId) {
        return getSelectorValue(String.valueOf(controlId));
    }

    public String getSelectorString(String controlId) {
        try {
            return get(controlId).items[get(controlId).current];
        } catch (Exception e) {
            DebugLog.panic("getSelectorString", e);
        }
        return null;
    }

    public String getSelectorString(int controlId) {
        return getSelectorString(String.valueOf(controlId));
    }

    public boolean getCheckBoxValue(String controlId) {
        try {
            return get(controlId).selected;
        } catch (Exception e) {
            DebugLog.panic("getChoiceItemValue", e);
        }
        return false;
    }

    public boolean getCheckBoxValue(int controlId) {
        return getCheckBoxValue(String.valueOf(controlId));
    }

    public void controlUpdated(BaseActivity activity, Control c) {
        if (null != controlListener) {
            controlListener.controlStateChanged(activity, c.id);
        }
    }
}