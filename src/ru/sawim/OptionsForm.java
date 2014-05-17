package ru.sawim;

import protocol.Protocol;
import protocol.xmpp.Xmpp;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.Util;
import ru.sawim.models.form.ControlStateListener;
import ru.sawim.models.form.Forms;
import ru.sawim.modules.Answerer;
import ru.sawim.modules.sound.Notify;
import ru.sawim.roster.RosterHelper;

public class OptionsForm implements ControlStateListener {

    public static final int OPTIONS_NETWORK = 0;
    public static final int OPTIONS_INTERFACE = 1;
    public static final int OPTIONS_SIGNALING = 2;
    public static final int OPTIONS_ANTISPAM = 3;
    public static final int OPTIONS_ANSWERER = 4;
    public static final int OPTIONS_PRO = 5;

    private Forms form;
    private int currentOptionsForm;

    private void setChecked(int lngStr, String optValue) {
        form.addCheckBox(optValue, lngStr, Options.getBoolean(optValue));
    }

    private void setChecked_(int lngStr, String optValue) {
        form.addCheckBox(optValue, lngStr, Options.getBoolean(optValue));
    }

    private void saveNotifyControls(String opt) {
        Options.setInt(opt, form.getCheckBoxValue(opt) ? 2 : 0);
    }

    private void saveOptionString(String opt) {
        Options.setString(opt, form.getTextFieldValue(opt));
    }

    private void saveOptionBoolean(String opt) {
        Options.setBoolean(opt, form.getCheckBoxValue(opt));
    }

    private void saveOptionSelector(String opt) {
        Options.setInt(opt, form.getSelectorValue(opt));
    }

    private void saveOptionGauge(String opt) {
        Options.setInt(opt, form.getVolumeValue(opt));
    }

    private void saveOptionFontGauge(String opt) {
        Options.setInt(opt, form.getGaugeValue(opt));
    }

    private void loadOptionInt(String opt, int label, String variants) {
        String current = String.valueOf(Options.getInt(opt));
        String[] alts = Util.explode(variants, '|');
        int selected = 0;
        for (int i = 0; i < alts.length; ++i) {
            if (alts[i].equals(current)) {
                selected = i;
            }
        }
        form.addSelector(opt, label, alts, selected);
    }

    private void saveOptionInt(String opt) {
        int val = Util.strToIntDef(form.getSelectorString(opt).trim(), 0);
        Options.setInt(opt, val);
    }

    private void save(int currentOptionsForm) {
        switch (currentOptionsForm) {
            case OPTIONS_NETWORK:
                saveOptionBoolean(Options.OPTION_INSTANT_RECONNECTION);
                saveOptionBoolean(Options.OPTION_WAKE_LOCK);
                break;

            case OPTIONS_INTERFACE:
                String[] colorSchemes = Scheme.getSchemeNames();
                if (1 < colorSchemes.length) {
                    Scheme.setColorScheme(form.getSelectorValue(Options.OPTION_COLOR_SCHEME));
                }
                saveOptionFontGauge(Options.OPTION_FONT_SCHEME);
                saveOptionBoolean(Options.OPTION_USER_GROUPS);
                saveOptionBoolean(Options.OPTION_SORT_UP_WITH_MSG);
                saveOptionBoolean(Options.OPTION_SHOW_STATUS_LINE);
                saveOptionSelector(Options.OPTION_CL_SORT_BY);
                saveOptionBoolean(Options.OPTION_HISTORY);
                saveOptionBoolean(Options.OPTION_HIDE_KEYBOARD);
                saveOptionBoolean(Options.OPTION_SIMPLE_INPUT);
                saveOptionInt(Options.OPTION_MAX_MSG_COUNT);
                saveOptionString(Options.UNAVAILABLE_NESSAGE);
                break;

            case OPTIONS_SIGNALING:
                saveOptionGauge(Options.OPTION_NOTIFY_VOLUME);
                saveOptionSelector(Options.OPTION_VIBRATOR);
                saveNotifyControls(Options.OPTION_ONLINE_NOTIF_MODE);
                saveNotifyControls(Options.OPTION_MESS_NOTIF_MODE);
                saveOptionBoolean(Options.OPTION_NOTIFY_IN_AWAY);
                saveOptionBoolean(Options.OPTION_ALARM);
                saveOptionBoolean(Options.OPTION_BLOG_NOTIFY);
                saveOptionSelector(Options.OPTION_TYPING_MODE);
                break;

            case OPTIONS_PRO:
                saveOptionBoolean(Options.OPTION_PUSH);
                saveOptionBoolean(Options.OPTION_HIDE_ICONS_CLIENTS);
                Options.setInt(Options.OPTION_AA_TIME, form.getSelectorValue(Options.OPTION_AA_TIME) * 5);
                break;

            case OPTIONS_ANTISPAM:
                saveOptionString(Options.OPTION_ANTISPAM_MSG);
                saveOptionString(Options.OPTION_ANTISPAM_ANSWER);
                saveOptionString(Options.OPTION_ANTISPAM_HELLO);
                saveOptionString(Options.OPTION_ANTISPAM_KEYWORDS);
                saveOptionBoolean(Options.OPTION_ANTISPAM_ENABLE);
                break;
        }
        Options.safeSave();
        SawimApplication.updateOptions();
    }

    public void select(BaseActivity activity, CharSequence name, int cmd) {
        currentOptionsForm = cmd;
        form = new Forms(R.string.options, null, false);
        form.setBackPressedListener(new Forms.OnBackPressed() {
            @Override
            public boolean back() {
                save(currentOptionsForm);
                return true;
            }
        });
        form.setCaption(name.toString());
        switch (currentOptionsForm) {
            case OPTIONS_NETWORK:
                setChecked(R.string.instant_reconnection, Options.OPTION_INSTANT_RECONNECTION);
                setChecked(R.string.wake_lock, Options.OPTION_WAKE_LOCK);
                break;

            case OPTIONS_INTERFACE:
                String[] colorSchemes = Scheme.getSchemeNames();
                if (colorSchemes.length > 1) {
                    form.addSelector(Options.OPTION_COLOR_SCHEME, R.string.color_scheme, colorSchemes, Options.getInt(Options.OPTION_COLOR_SCHEME));
                }
                form.addFontVolumeControl(Options.OPTION_FONT_SCHEME, R.string.fonts, Options.getInt(Options.OPTION_FONT_SCHEME));

                form.addString(R.string.contact_list, null);
                setChecked(R.string.show_user_groups, Options.OPTION_USER_GROUPS);

                setChecked(R.string.show_status_line, Options.OPTION_SHOW_STATUS_LINE);
                setChecked(R.string.contacts_with_msg_at_top, Options.OPTION_SORT_UP_WITH_MSG);
                int[] sort = {R.string.sort_by_status, R.string.sort_by_online, R.string.sort_by_name};
                form.addSelector(Options.OPTION_CL_SORT_BY, R.string.sort_by, sort, Options.getInt(Options.OPTION_CL_SORT_BY));

                form.addString(R.string.chat, null);
                setChecked(R.string.hide_chat_keyboard, Options.OPTION_HIDE_KEYBOARD);
                setChecked(R.string.use_simple_input, Options.OPTION_SIMPLE_INPUT);
                setChecked(R.string.use_history, Options.OPTION_HISTORY);
                loadOptionInt(Options.OPTION_MAX_MSG_COUNT, R.string.max_message_count, "10|50|100|250|500|1000");

                form.addTextField(Options.UNAVAILABLE_NESSAGE, R.string.post_outputs, Options.getString(Options.UNAVAILABLE_NESSAGE));
                break;

            case OPTIONS_SIGNALING:
                if (Notify.getSound().hasAnySound()) {
                    form.addVolumeControl_(Options.OPTION_NOTIFY_VOLUME, R.string.volume, Options.getInt(Options.OPTION_NOTIFY_VOLUME));
                }

                form.addCheckBox(Options.OPTION_MESS_NOTIF_MODE, R.string.message_notification, 0 < Options.getInt(Options.OPTION_MESS_NOTIF_MODE));
                form.addCheckBox(Options.OPTION_ONLINE_NOTIF_MODE, R.string.onl_notification, 0 < Options.getInt(Options.OPTION_ONLINE_NOTIF_MODE));
                setChecked(R.string.alarm, Options.OPTION_ALARM);
                setChecked(R.string.blog_notify, Options.OPTION_BLOG_NOTIFY);

                int[] typingItems = {R.string.no, R.string.typing_incoming, R.string.typing_both};
                form.addSelector(Options.OPTION_TYPING_MODE, R.string.typing_notify, typingItems, Options.getInt(Options.OPTION_TYPING_MODE));
                int[] vibrationItems = {R.string.no, R.string.yes, R.string.when_locked};
                form.addSelector(Options.OPTION_VIBRATOR, R.string.vibration, vibrationItems, Options.getInt(Options.OPTION_VIBRATOR));
                setChecked(R.string.notify_in_away, Options.OPTION_NOTIFY_IN_AWAY);
                break;

            case OPTIONS_ANTISPAM:
                setChecked(R.string.on, Options.OPTION_ANTISPAM_ENABLE);
                form.addTextField(Options.OPTION_ANTISPAM_MSG, R.string.antispam_msg, Options.getString(Options.OPTION_ANTISPAM_MSG));
                form.addTextField(Options.OPTION_ANTISPAM_ANSWER, R.string.antispam_answer, Options.getString(Options.OPTION_ANTISPAM_ANSWER));
                form.addTextField(Options.OPTION_ANTISPAM_HELLO, R.string.antispam_hello, Options.getString(Options.OPTION_ANTISPAM_HELLO));
                form.addTextField(Options.OPTION_ANTISPAM_KEYWORDS, R.string.antispam_keywords, Options.getString(Options.OPTION_ANTISPAM_KEYWORDS));
                break;

            case OPTIONS_PRO:
                setChecked_(R.string.push, Options.OPTION_PUSH);
                setChecked_(R.string.hide_icons_clients, Options.OPTION_HIDE_ICONS_CLIENTS);
                form.addSelector(Options.OPTION_AA_TIME, JLocale.getString(R.string.absence)
                        + " " + JLocale.getString(R.string.after_time), JLocale.getString(R.string.off) + "|5 |10 |15 ", Options.getInt(Options.OPTION_AA_TIME) / 5);
                break;

            case OPTIONS_ANSWERER:
                Answerer.getInstance().activate();
                return;
        }
        form.setControlStateListener(this);
        form.preferenceFormShow(activity);
    }

    public void controlStateChanged(String id) {
        if (id.equals(Options.OPTION_FONT_SCHEME)) {
            saveOptionSelector(Options.OPTION_FONT_SCHEME);
        } else if (id.equals(Options.OPTION_PUSH)) {
            int count = RosterHelper.getInstance().getProtocolCount();
            for (int i = 0; i < count; ++i) {
                Protocol p = RosterHelper.getInstance().getProtocol(i);
                if (p instanceof Xmpp) {
                    if (form.getCheckBoxValue(Options.OPTION_PUSH)) {
                        SawimApplication.getInstance().getXmppSession().pushRegister(((Xmpp) p).getConnection());
                    } else {
                        SawimApplication.getInstance().getXmppSession().pushUnregister(((Xmpp) p).getConnection());
                    }
                }
            }
        }
    }
}