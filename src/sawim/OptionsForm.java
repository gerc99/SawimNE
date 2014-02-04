package sawim;

import ru.sawim.SawimApplication;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.form.ControlStateListener;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import sawim.comm.Util;
import sawim.modules.Answerer;
import sawim.modules.AutoAbsence;
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

    private void setChecked(String lngStr, int optValue) {
        form.addCheckBox(optValue, lngStr, Options.getBoolean(optValue));
    }

    private void setChecked_(String lngStr, int optValue) {
        form.addCheckBox(optValue, lngStr, Options.getBoolean(optValue));
    }

    private void createNotifyControls(int modeOpt, String title) {
        form.addCheckBox(modeOpt, title, 0 < Options.getInt(modeOpt));
    }

    private void saveNotifyControls(int opt) {
        Options.setInt(opt, form.getCheckBoxValue(opt) ? 2 : 0);
    }

    private void createSelector(String cap, String items, int opt) {
        form.addSelector(opt, cap, items, Options.getInt(opt));
    }

    private void loadOptionString(int opt, String label) {
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

    private void loadOptionGauge(int opt, String label) {
        form.addVolumeControl(opt, label, Options.getInt(opt));
    }

    private void loadOptionFontGauge(int opt, String label) {
        form.addFontVolumeControl(opt, label, Options.getInt(opt));
    }

    private void saveOptionGauge(int opt) {
        Options.setInt(opt, form.getVolumeValue(opt));
    }

    private void saveOptionFontGauge(int opt) {
        Options.setInt(opt, form.getGaugeValue(opt));
    }

    private void loadOptionInt(int opt, String label, String variants) {
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

    private void loadOptionInt(int opt, String label, String[] variants, short[] alts) {
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
        form = new Forms(SawimApplication.getContext().getString(R.string.options), null, false);
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
                    form.addSelector(Options.OPTION_COLOR_SCHEME, "color_scheme", colorSchemes, Options.getInt(Options.OPTION_COLOR_SCHEME));
                }
            /*    if (JLocale.langAvailable.length > 1) {
                    int cur = 0;
                    String curLang = Options.getString(Options.OPTION_UI_LANGUAGE);
                    for (int j = 0; j < JLocale.langAvailable.length; ++j) {
                        if (JLocale.langAvailable[j].equals(curLang)) {
                            cur = j;
                        }
                    }
                    form.addSelector(Options.OPTION_UI_LANGUAGE, "language", JLocale.langAvailableName, cur);
                }*/
                loadOptionFontGauge(Options.OPTION_FONT_SCHEME, "fonts");

                form.addString("contact_list", null);
                setChecked("show_user_groups", Options.OPTION_USER_GROUPS);

                //createSelector("show_user_accounts",
                //        "no" + "|" + "by_groups" + "|" + "by_windows", Options.OPTION_USER_ACCOUNTS);

                //setChecked("hide_offline", Options.OPTION_CL_HIDE_OFFLINE);
                //        setChecked("save_temp_contacts", Options.OPTION_SAVE_TEMP_CONTACT);
                setChecked("show_status_line", Options.OPTION_SHOW_STATUS_LINE);
                setChecked("contacts_with_msg_at_top", Options.OPTION_SORT_UP_WITH_MSG);

                createSelector("sort_by",
                        "sort_by_status" + "|" + "sort_by_online" + "|" + "sort_by_name",
                        Options.OPTION_CL_SORT_BY);

                form.addString("chat", null);
                //setChecked("show_platform", Options.OPTION_SHOW_PLATFORM);
                setChecked(SawimApplication.getContext().getString(R.string.hide_chat_keyboard), Options.OPTION_HIDE_KEYBOARD);
                setChecked("use_simple_input", Options.OPTION_SIMPLE_INPUT);
                setChecked("use_history", Options.OPTION_HISTORY);
                loadOptionInt(Options.OPTION_MAX_MSG_COUNT, "max_message_count", "10|50|100|250|500|1000");

                setChecked("title_in_conference", Options.OPTION_TITLE_IN_CONFERENCE);
                loadOptionString(Options.UNAVAILABLE_NESSAGE, "post_outputs");
                form.addString("options_network", null);
                setChecked(SawimApplication.getContext().getString(R.string.instant_reconnection), Options.OPTION_INSTANT_RECONNECTION);
                break;

            case OPTIONS_SIGNALING:
                if (Notify.getSound().hasAnySound()) {
                    loadOptionGauge(Options.OPTION_NOTIFY_VOLUME, "volume");
                }

                createNotifyControls(Options.OPTION_MESS_NOTIF_MODE,
                        "message_notification");
                createNotifyControls(Options.OPTION_ONLINE_NOTIF_MODE,
                        "onl_notification");
                setChecked("alarm", Options.OPTION_ALARM);
                setChecked("blog_notify", Options.OPTION_BLOG_NOTIFY);

                createSelector("typing_notify",
                        "no" + "|" + "typing_incoming" + "|" + "typing_both",
                        Options.OPTION_TYPING_MODE);
                createSelector(
                        "vibration",
                        "no" + "|" + "yes" + "|" + "when_locked",
                        Options.OPTION_VIBRATOR);
                setChecked("notify_in_away", Options.OPTION_NOTIFY_IN_AWAY);
                break;

            case OPTIONS_ANTISPAM:
                setChecked("on", Options.OPTION_ANTISPAM_ENABLE);
                loadOptionString(Options.OPTION_ANTISPAM_MSG, "antispam_msg");
                loadOptionString(Options.OPTION_ANTISPAM_ANSWER, "antispam_answer");
                loadOptionString(Options.OPTION_ANTISPAM_HELLO, "antispam_hello");
                loadOptionString(Options.OPTION_ANTISPAM_KEYWORDS, "antispam_keywords");
                break;

            case OPTIONS_PRO:
                setChecked_(SawimApplication.getContext().getString(R.string.hide_icons_clients), Options.OPTION_HIDE_ICONS_CLIENTS);
                form.addSelector(Options.OPTION_AA_TIME, SawimApplication.getContext().getString(R.string.absence)
                        + " " + JLocale.getString("after_time"), "off" + "|5 |10 |15 ", Options.getInt(Options.OPTION_AA_TIME) / 5);
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