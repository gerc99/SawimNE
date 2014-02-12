package sawim.modules.tracking;

import DrawControls.icons.Icon;
import android.graphics.drawable.Drawable;
import android.util.Log;
import ru.sawim.SawimApplication;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListItem;
import ru.sawim.models.list.VirtualListModel;
import ru.sawim.view.TextBoxView;
import sawim.util.JLocale;

import java.util.Vector;

public final class TrackingForm implements TextBoxView.TextBoxListener {

    private static final int NO = 0;
    private static final int YES = 1;
    private static final int NOTHING = 2;

    public static final Drawable No = SawimApplication.getContext().getResources().getDrawable(android.R.drawable.checkbox_off_background);
    public static final Drawable Yes = SawimApplication.getContext().getResources().getDrawable(android.R.drawable.checkbox_on_background);
    private static final Drawable Nothing = SawimApplication.getContext().getResources().getDrawable(android.R.drawable.radiobutton_off_background);
    private static final Drawable Track = SawimApplication.getContext().getResources().getDrawable(android.R.drawable.ic_menu_search);
    private static Vector list = new Vector();
    private String uin;
    private VirtualList screen = VirtualList.getInstance();
    private VirtualListModel model = new VirtualListModel();
    private TextBoxView inputBox;
    public static String editText = null;

    public TrackingForm(String uin) {
        this.uin = uin;
        screen.setCaption(JLocale.getString(R.string.extra_settings));
        screen.setModel(model);
        screen.setClickListListener(new VirtualList.OnClickListListener() {
            @Override
            public void itemSelected(int position) {
                changeStatus(position);
            }

            @Override
            public boolean back() {
                saveList();
                screen.updateModel();
                return true;
            }
        });
    }

    public static Drawable getTrackIcon() {
        return Track;
    }

    public static Vector getList() {
        return list;
    }

    private String getName(int key) {
        return JLocale.getString(key);
    }

    private Line createLine(int id_event, int id_action, Drawable s_icon, Icon l_icon, String name, boolean isAll, boolean isEvent) {
        Line line = new Line();
        line.id_event = id_event;
        line.id_action = id_action;
        line.status_flag = NO;
        line.status_icon = s_icon;
        line.line_icon = l_icon;
        line.name = name;
        line.isAll = isAll;
        line.isEvent = isEvent;
        return line;
    }

    private Line createLine(int id_event, int id_action, Drawable s_icon, Icon l_icon, String name, boolean isEvent) {
        Line line = new Line();
        line.id_event = id_event;
        line.id_action = id_action;
        line.status_flag = NO;
        line.status_icon = s_icon;
        line.line_icon = l_icon;
        line.name = name;
        line.isAll = false;
        line.isEvent = isEvent;
        return line;
    }

    private void setLine(Line line, int index) {
        list.setElementAt(line, index);
    }

    private void setLineByID(Line line_new, int evt_index, int act_index) {
        for (int i = 0; i < list.size(); i++) {
            Line line = getLine(i);
            if (line.id_event == evt_index && line.id_action == act_index) {
                setLine(line_new, i);
                break;
            }
        }
    }

    private Line getLine(int index) {
        return (Line) list.elementAt(index);
    }

    private Line getLineByID(int evt_index, int act_index) {
        Line l = null;
        for (int i = 0; i < list.size(); i++) {
            Line line = getLine(i);
            if (line.id_event == evt_index && line.id_action == act_index) {
                l = line;
            }
        }
        return l;
    }

    private void createListForConf() {
        list.removeAllElements();
        list.addElement(createLine(Tracking.GLOBAL, Tracking.GLOBAL, No, null, getName(R.string.extra_settings), true, false));
        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.EVENT_ENTER, No, null, getName(R.string.track_event_enter), true));
        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.ACTION_HISTORY, No, null, getName(R.string.use_history)
                + " (" + SawimApplication.getInstance().getResources().getString(R.string.include_setting_the_story) + ")", false));
        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.ACTION_PRESENCE, No, null, getName(R.string.notice_presence), false));
        list.addElement(createLine(Tracking.EVENT_MESSAGE, Tracking.EVENT_MESSAGE, No, null, getName(R.string.track_event_message), true));
        list.addElement(createLine(Tracking.EVENT_MESSAGE, Tracking.ACTION_SOUND, No, null, getName(R.string.track_action_sound), false));
        list.addElement(createLine(Tracking.EVENT_MESSAGE, Tracking.ACTION_VIBRA, No, null, getName(R.string.track_action_vibra), false));
    }

    private void createList() {
        list.removeAllElements();
        list.addElement(createLine(Tracking.GLOBAL, Tracking.GLOBAL, No, null, getName(R.string.extra_settings), true, false));

        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.EVENT_ENTER, No, null, getName(R.string.track_event_enter), true));
        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.ACTION_HISTORY, No, null, getName(R.string.use_history), false));
        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.ACTION_CHAT, No, null, getName(R.string.track_action_chat), false));
        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.ACTION_NOTICE, No, null, getName(R.string.track_action_notice), false));
        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.ACTION_ICON, No, null, getName(R.string.track_action_icon), false));
        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.ACTION_INCHAT, No, null, getName(R.string.track_action_inchat), false));

        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.ACTION_SOUND, No, null, getName(R.string.track_action_sound), false));
        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.ACTION_VIBRA, No, null, getName(R.string.track_action_vibra), false));
        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.ACTION_MESSAGE, No, null, getName(R.string.track_action_message), false));
        list.addElement(createLine(Tracking.EVENT_ENTER, Tracking.ACTION_MESSAGE_TEXT, null, null, getName(R.string.message), false));

        list.addElement(createLine(Tracking.EVENT_EXIT, Tracking.EVENT_EXIT, No, null, getName(R.string.track_event_exit), true));
        list.addElement(createLine(Tracking.EVENT_EXIT, Tracking.ACTION_NOTICE, No, null, getName(R.string.track_action_notice), false));
        list.addElement(createLine(Tracking.EVENT_EXIT, Tracking.ACTION_ICON, No, null, getName(R.string.track_action_icon), false));
        list.addElement(createLine(Tracking.EVENT_EXIT, Tracking.ACTION_INCHAT, No, null, getName(R.string.track_action_inchat), false));

        list.addElement(createLine(Tracking.EVENT_EXIT, Tracking.ACTION_SOUND, No, null, getName(R.string.track_action_sound), false));
        list.addElement(createLine(Tracking.EVENT_EXIT, Tracking.ACTION_VIBRA, No, null, getName(R.string.track_action_vibra), false));

        list.addElement(createLine(Tracking.EVENT_MESSAGE, Tracking.EVENT_MESSAGE, No, null, getName(R.string.track_event_message), true));
        list.addElement(createLine(Tracking.EVENT_MESSAGE, Tracking.ACTION_SOUND, No, null, getName(R.string.track_action_sound), false));
        list.addElement(createLine(Tracking.EVENT_MESSAGE, Tracking.ACTION_VIBRA, No, null, getName(R.string.track_action_vibra), false));

        list.addElement(createLine(Tracking.EVENT_TYPING, Tracking.EVENT_TYPING, No, null, getName(R.string.track_event_typing), true));
        list.addElement(createLine(Tracking.EVENT_TYPING, Tracking.ACTION_SOUND, No, null, getName(R.string.track_action_sound), false));
        list.addElement(createLine(Tracking.EVENT_TYPING, Tracking.ACTION_VIBRA, No, null, getName(R.string.track_action_vibra), false));
    }

    private void updateMassMenu() {
    }

    private Drawable convertStatusToIcon(int status) {
        Drawable icon = No;
        switch (status) {
            case NO:
                icon = No;
                break;
            case YES:
                icon = Yes;
                break;
            case NOTHING:
                icon = Nothing;
                break;
        }
        return icon;
    }

    private void fillList() {
        clearStatusList();
        Vector actions = Tracking.getTrackList(uin);
        if (actions == null || actions.size() <= 0) return;
        for (int i = 0; i < actions.size(); i++) {
            Tracking.Track track = (Tracking.Track) actions.elementAt(i);
            Line line = getLineByID(track.idEvent, track.idAction);
            if (line == null) continue;
            if (track.idAction == Tracking.ACTION_MESSAGE_TEXT) {
                editText = track.valueAction;
                if (line.name.length() > 0) {
                    line.status_flag = YES;
                } else {
                    line.status_flag = NO;
                }
            } else {
                line.status_flag = YES;
                line.status_icon = convertStatusToIcon(line.status_flag);
            }
            boolean is = line.id_action == Tracking.EVENT_ENTER;
            setLineByID(line, line.id_event, line.id_action);
        }
        setStatusEvent();
    }

    private void clearStatusList() {
        for (int i = 0; i < list.size(); i++) {
            Line line = getLine(i);
            line.status_flag = NO;
            if (line.id_action != Tracking.ACTION_MESSAGE_TEXT) {
                line.status_icon = convertStatusToIcon(line.status_flag);
            }
            setLine(line, i);
        }
    }

    private void saveList() {
        Tracking.addTrackList(uin);
        Tracking.saveTrackingToRMS();
    }

    private synchronized void showList() {
        model.clear();
        VirtualListItem record = model.createNewParser(true);
        Line line = getLine(0);
        if (line.status_icon != null) {
            record.addImage(line.status_icon);
        }
        if (line.line_icon != null) {
            record.addImage(line.line_icon.getImage());
        }
        record.addDescription(line.name, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        model.addPar(record);
        if (line.status_flag == YES) {
            for (int i = 1; i < list.size(); i++) {
                addTextToForm(i);
            }
        }
        screen.updateModel();
    }

    private void addTextToForm(int index) {
        VirtualListItem record = model.createNewParser(true);
        Line line = getLine(index);
        if (line.status_icon != null) {
            record.addImage(line.status_icon);
        }
        if (line.line_icon != null) {
            record.addImage(line.line_icon.getImage());
        }
        if (line.isEvent) {
            record.addDescription(20, line.name, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        }
        if (!line.isEvent && !line.isAll) {
            record.addDescription(40, line.name, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        }
        model.addPar(record);
    }

    public void activate() {
        createList();
        fillList();
        screen.show();
        showList();
    }

    public void activateForConf() {
        updateMassMenu();
        createListForConf();
        fillList();
        screen.show();
        showList();
    }

    private void activateInputBox(String text) {
        if (inputBox == null) {
            inputBox = new TextBoxView();
            inputBox.setTextBoxListener(this);
            inputBox.setCaption(null);
        }
        inputBox.setString(text);
        inputBox.show(SawimApplication.getCurrentActivity().getSupportFragmentManager(), "tracking-message");
    }

    private void changeStatus(int index) {
        Line line = getLine(index);
        if (line.id_action == Tracking.ACTION_MESSAGE_TEXT) {
            activateInputBox(editText);
        } else if (line.isAll) {
            if (line.status_flag == NO || line.status_flag == NOTHING) {
                line.status_flag = YES;
            } else {
                line.status_flag = NO;
            }
            setLine(line, 0);
            line.status_icon = convertStatusToIcon(line.status_flag);
            setLine(line, 0);
        } else if (line.isEvent) {
            if (line.status_flag == NO || line.status_flag == NOTHING) {
                line.status_flag = YES;
            } else {
                line.status_flag = NO;
            }

            setLine(line, index);
            for (int i = index; i < list.size(); i++) {
                Line act_line = getLine(i);
                if (i != index && act_line.isEvent && !act_line.isAll) break;
                if (act_line.status_icon == null) continue;
                act_line.status_flag = line.status_flag;
                act_line.status_icon = convertStatusToIcon(act_line.status_flag);
                setLine(act_line, i);
            }

        } else {
            if (line.status_flag == NO) line.status_flag = YES;
            else line.status_flag = NO;
            line.status_icon = convertStatusToIcon(line.status_flag);
            setLine(line, index);
            int evt_index = getIndexEvent(index);
            Line evt_line = getLine(evt_index);
            evt_line.status_flag = getStatusEvent(evt_index);
            evt_line.status_icon = convertStatusToIcon(evt_line.status_flag);
            setLine(evt_line, evt_index);
        }
        showList();
        updateMassMenu();
    }

    private int getIndexEvent(int index) {
        for (int i = index; i >= 0; i--) {
            Line line = getLine(i);
            if (line.isEvent) {
                index = i;
                break;
            }
        }
        return index;
    }

    private int getStatusEvent(int index) {
        int ret = 0;
        boolean flag = false;
        boolean isZero = false;
        index++;
        for (int i = index; i < list.size(); i++) {
            Line line = getLine(i);
            if (line.status_icon == null) continue;
            if (line.isEvent) break;
            boolean select = convertIntToBool(line.status_flag);
            flag |= select;
            if (!select) isZero = true;
        }
        if (!flag) ret = NO;
        if (flag && !isZero) ret = YES;
        if (flag && isZero) ret = NOTHING;
        return ret;
    }

    private void setStatusEvent() {
        for (int i = 0; i < list.size(); i++) {
            Line line = getLine(i);
            if (!line.isEvent) continue;
            int status = getStatusEvent(i);
            line.status_flag = status;
            line.status_icon = convertStatusToIcon(line.status_flag);
            setLine(line, i);
        }
    }

    private boolean convertIntToBool(int i) {
        boolean ret = false;
        if (i == YES || i == NOTHING) {
            ret = true;
        }
        return ret;
    }

    public void textboxAction(TextBoxView box, boolean ok) {
        if ((box == inputBox) && ok) {
            setLineText(inputBox.getString());
            return;
        }
    }

    private void setLineText(String text) {
        int index = screen.getCurrItem();
        Line line = getLine(index);
        //if (line.id_action != Tracking.ACTION_MESSAGE_TEXT) {
        //    return;
        //}
        editText = text;
        /*setLine(line, index);
        index--;
        Line prev_line = getLine(index);
        if (text.length() > 0) {
            prev_line.status_flag = YES;
            line.status_flag = YES;
        } else {
            prev_line.status_flag = NO;
            line.status_flag = NO;
        }
        prev_line.status_icon = convertStatusToIcon(prev_line.status_flag);
        setLine(prev_line, index);*/
        showList();

        updateMassMenu();
    }

    public static class Line {
        public int id_event;
        public int id_action;
        public Drawable status_icon;
        public Icon line_icon;
        public String name;
        public boolean isAll;
        public boolean isEvent;
        public int status_flag;
    }
}


