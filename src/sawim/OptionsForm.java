package sawim;

import ru.sawim.SawimApplication;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.models.form.ControlStateListener;
import ru.sawim.models.form.Forms;
import sawim.comm.Util;
import sawim.modules.Answerer;
import sawim.modules.Notify;
import sawim.util.JLocale;

public class OptionsForm implements ControlStateListener {

    private Forms form;
    private int currentOptionsForm;

    public static final int OPTIONS_INTERFACE = 0;
    public static final int OPTIONS_SIGNALING = 1;
    public static final int OPTIONS_ANTISPAM = 2;
    public static final int OPTIONS_ANSWERER = 3;
    public static final int OPTIONS_PRO = 4;

    private void setChecked(int lngStr, int optValue) {
        form.addCheckBox(optValue, lngStr, Options.getBoolean(optValue));
    }

    private void setChecked_(int lngStr, int optValue) {
        form.addCheckBox(optValue, lngStr, Options.getBoolean(optValue));
    }

    private void createNotifyControls(int modeOpt, int title) {
        form.addCheckBox(modeOpt, title, 0 < Options.getInt(modeOpt));
    }

    private void saveNotifyControls(int opt) {
        Options.setInt(opt, form.getCheckBoxValue(opt) ? 2 : 0);
    }

    private void createSelector(int cap, int[] items, int opt) {
        form.addSelector(opt, cap, items, Options.getInt(opt));
    }

    private void loadOptionString(int opt, int label) {
        form.addTextField(opt, label, Options.getString(opt));
    }

    private void saveOptionString(int opt) {
        Options.setString(opt, form.getTextFieldValue(opt));
    }

    private void saveOptionBoolean(int opt) {
        Options.setBoolean(opt, form.getCheckBoxValue(opt));
    }

    private void saveOptionSelector(int opt) {
        Options.setInt(opt, form.getSelectorValue(opt));
    }

    private void loadOptionGauge(int opt, int label) {
        form.addVolumeControl_(opt, label, Options.getInt(opt));
    }

    private void loadOptionFontGauge(int opt, int label) {
        form.addFontVolumeControl(opt, label, Options.getInt(opt));
    }

    private void saveOptionGauge(int opt) {
        Options.setInt(opt, form.getVolumeValue(opt));
    }

    private void saveOptionFontGauge(int opt) {
        Options.setInt(opt, form.getGaugeValue(opt));
    }

    private void loadOptionInt(int opt, int label, String variants) {
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

    private void saveOptionInt(int opt) {
        int val = Util.strToIntDef(form.getSelectorString(opt).trim(), 0);
        Options.setInt(opt, val);
    }

    private void loadOptionInt(int opt, int label, String[] variants, short[] alts) {
        int current = Options.getInt(opt);
        int selected = 0;
        for (int i = 0; i < alts.length; ++i) {
            if (alts[i] == current) {
                selected = i;
            }
        }
        form.addSelector(opt, label, variants, selected);
    }

    private void saveOptionInt(int opt, short[] alts) {
        Options.setInt(opt, alts[form.getSelectorValue(opt)]);
    }

    private void save(int currentOptionsForm) {
        switch (currentOptionsForm) {
            case OPTIONS_INTERFACE:
                String[] colorSchemes = Scheme.getSchemeNames();
                if (1 < colorSchemes.length) {
                    saveOptionSelector(Options.OPTION_COLOR_SCHEME);
                    Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
                }
                //if (JLocale.langAvailable.length > 1) {
                //    int lang = form.getSelectorValue(Options.OPTION_UI_LANGUAGE);
                //    Options.setString(Options.OPTION_UI_LANGUAGE, JLocale.langAvailable[lang]);
                //}

                saveOptionFontGauge(Options.OPTION_FONT_SCHEME);
                //GraphicsEx.setFontScheme(Options.getInt(Options.OPTION_FONT_SCHEME));
                //saveOptionInt(Options.OPTION_MIN_ITEM_SIZE, minItemMultipliers);
                //Scheme.updateUI();

                saveOptionBoolean(Options.OPTION_USER_GROUPS);
                //saveOptionSelector(Options.OPTION_USER_ACCOUNTS);
                //saveOptionBoolean(Options.OPTION_CL_HIDE_OFFLINE);
                //saveOptionBoolean(Options.OPTION_SAVE_TEMP_CONTACT);
                saveOptionBoolean(Options.OPTION_SORT_UP_WITH_MSG);
                saveOptionBoolean(Options.OPTION_SHOW_STATUS_LINE);
                saveOptionSelector(Options.OPTION_CL_SORT_BY);
                //saveOptionBoolean(Options.OPTION_SHOW_PLATFORM);
                saveOptionBoolean(Options.OPTION_HISTORY);
                saveOptionBoolean(Options.OPTION_HIDE_KEYBOARD);
                saveOptionBoolean(Options.OPTION_SIMPLE_INPUT);
                saveOptionInt(Options.OPTION_MAX_MSG_COUNT);
                saveOptionBoolean(Options.OPTION_TITLE_IN_CONFERENCE);
                saveOptionString(Options.UNAVAILABLE_NESSAGE);
                saveOptionBoolean(Options.OPTION_INSTANT_RECONNECTION);
                saveOptionBoolean(Options.OPTION_WAKE_LOCK);
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

    public void select(CharSequence name, int cmd) {
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
            case OPTIONS_INTERFACE:
                String[] colorSchemes = Scheme.getSchemeNames();
                if (colorSchemes.length > 1) {
                    form.addSelector(Options.OPTION_COLOR_SCHEME, R.string.color_scheme, colorSchemes, Options.getInt(Options.OPTION_COLOR_SCHEME));
                }
                loadOptionFontGauge(Options.OPTION_FONT_SCHEME, R.string.fonts);

                form.addString(R.string.contact_list, null);
                setChecked(R.string.show_user_groups, Options.OPTION_USER_GROUPS);

                //createSelector("show_user_accounts",
                //        "no" + "|" + "by_groups" + "|" + "by_windows", Options.OPTION_USER_ACCOUNTS);

                //setChecked("hide_offline", Options.OPTION_CL_HIDE_OFFLINE);
                //setChecked("save_temp_contacts", Options.OPTION_SAVE_TEMP_CONTACT);
                setChecked(R.string.show_status_line, Options.OPTION_SHOW_STATUS_LINE);
                setChecked(R.string.contacts_with_msg_at_top, Options.OPTION_SORT_UP_WITH_MSG);
                int[] sort = {R.string.sort_by_status, R.string.sort_by_online, R.string.sort_by_name};
                createSelector(R.string.sort_by, sort, Options.OPTION_CL_SORT_BY);

                form.addString(R.string.chat, null);
                //setChecked("show_platform", Options.OPTION_SHOW_PLATFORM);
                setChecked(R.string.hide_chat_keyboard, Options.OPTION_HIDE_KEYBOARD);
                setChecked(R.string.use_simple_input, Options.OPTION_SIMPLE_INPUT);
                setChecked(R.string.use_history, Options.OPTION_HISTORY);
                loadOptionInt(Options.OPTION_MAX_MSG_COUNT, R.string.max_message_count, "10|50|100|250|500|1000");

                setChecked(R.string.title_in_conference, Options.OPTION_TITLE_IN_CONFERENCE);
                loadOptionString(Options.UNAVAILABLE_NESSAGE, R.string.post_outputs);
                form.addString(R.string.options_network, null);
                setChecked(R.string.instant_reconnection, Options.OPTION_INSTANT_RECONNECTION);
                setChecked(R.string.wake_lock, Options.OPTION_WAKE_LOCK);
                break;

            case OPTIONS_SIGNALING:
                if (Notify.getSound().hasAnySound()) {
                    loadOptionGauge(Options.OPTION_NOTIFY_VOLUME, R.string.volume);
                }

                createNotifyControls(Options.OPTION_MESS_NOTIF_MODE,
                        R.string.message_notification);
                createNotifyControls(Options.OPTION_ONLINE_NOTIF_MODE,
                        R.string.onl_notification);
                setChecked(R.string.alarm, Options.OPTION_ALARM);
                setChecked(R.string.blog_notify, Options.OPTION_BLOG_NOTIFY);

                int[] typingItems = {R.string.no, R.string.typing_incoming, R.string.typing_both};
                createSelector(R.string.typing_notify, typingItems, Options.OPTION_TYPING_MODE);
                int[] vibrationItems = {R.string.no, R.string.yes, R.string.when_locked};
                createSelector(R.string.vibration, vibrationItems, Options.OPTION_VIBRATOR);
                setChecked(R.string.notify_in_away, Options.OPTION_NOTIFY_IN_AWAY);
                break;

            case OPTIONS_ANTISPAM:
                setChecked(R.string.on, Options.OPTION_ANTISPAM_ENABLE);
                loadOptionString(Options.OPTION_ANTISPAM_MSG, R.string.antispam_msg);
                loadOptionString(Options.OPTION_ANTISPAM_ANSWER, R.string.antispam_answer);
                loadOptionString(Options.OPTION_ANTISPAM_HELLO, R.string.antispam_hello);
                loadOptionString(Options.OPTION_ANTISPAM_KEYWORDS, R.string.antispam_keywords);
                break;

            case OPTIONS_PRO:
                setChecked_(R.string.hide_icons_clients, Options.OPTION_HIDE_ICONS_CLIENTS);
                form.addSelector(Options.OPTION_AA_TIME, JLocale.getString(R.string.absence)
                        + " " + JLocale.getString(R.string.after_time), JLocale.getString(R.string.off) + "|5 |10 |15 ", Options.getInt(Options.OPTION_AA_TIME) / 5);
                break;

            case OPTIONS_ANSWERER:
                Answerer.getInstance().activate();
                return;
        }
        form.setControlStateListener(this);
        form.preferenceFormShow();
    }

    public void controlStateChanged(int id) {
        switch (id) {
            case Options.OPTION_FONT_SCHEME:
                saveOptionSelector(Options.OPTION_FONT_SCHEME);
                break;
        }
    }
}