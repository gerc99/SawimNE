package sawim;

import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import sawim.cl.ContactList;
import sawim.comm.*;
import sawim.modules.*;
import sawim.util.*;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.form.ControlStateListener;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;

public class OptionsForm implements FormListener, ControlStateListener {

    private Forms form;
    private int currentOptionsForm;

    public static final int OPTIONS_ACCOUNT    = 7;
    public static final int OPTIONS_INTERFACE  = 8;
    public static final int OPTIONS_SIGNALING  = 9;
    public static final int OPTIONS_ANTISPAM   = 10;
    public static final int OPTIONS_ABSENCE    = 11;
    public static final int OPTIONS_ANSWERER   = 12;

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

    private void saveOptionGauge(int opt) {
        Options.setInt(opt, form.getVolumeValue(opt));
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
                if (JLocale.langAvailable.length > 1) {
                    int lang = form.getSelectorValue(Options.OPTION_UI_LANGUAGE);
                    Options.setString(Options.OPTION_UI_LANGUAGE, JLocale.langAvailable[lang]);
                }

                saveOptionSelector(Options.OPTION_FONT_SCHEME);
                //GraphicsEx.setFontScheme(Options.getInt(Options.OPTION_FONT_SCHEME));
                //saveOptionInt(Options.OPTION_MIN_ITEM_SIZE, minItemMultipliers);
                //Scheme.updateUI();

                saveOptionBoolean(Options.OPTION_USER_GROUPS);
                saveOptionBoolean(Options.OPTION_HIDE_ICONS_CLIENTS);
                //saveOptionSelector(Options.OPTION_USER_ACCOUNTS);
                //saveOptionBoolean(Options.OPTION_CL_HIDE_OFFLINE);
                saveOptionBoolean(Options.OPTION_SAVE_TEMP_CONTACT);
                saveOptionBoolean(Options.OPTION_SORT_UP_WITH_MSG);
                saveOptionBoolean(Options.OPTION_SHOW_STATUS_LINE);
                saveOptionSelector(Options.OPTION_CL_SORT_BY);
                saveOptionBoolean(Options.OPTION_SHOW_PLATFORM);
                saveOptionBoolean(Options.OPTION_HISTORY);
                //saveOptionSelector(Options.OPTION_CHAT_PRESENSEFONT_SCHEME);
                //GraphicsEx.setChatPresenseFont(Options.getInt(Options.OPTION_CHAT_PRESENSEFONT_SCHEME));
                saveOptionBoolean(Options.OPTION_TITLE_IN_CONFERENCE);
                saveOptionString(Options.UNAVAILABLE_NESSAGE);
                saveOptionBoolean(Options.OPTION_SIMPLE_INPUT);
                saveOptionInt(Options.OPTION_MAX_MSG_COUNT);

                ContactList.getInstance().getManager().update();
                SawimActivity.getInstance().recreateActivity();
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

            case OPTIONS_ANTISPAM:
                saveOptionString(Options.OPTION_ANTISPAM_MSG);
                saveOptionString(Options.OPTION_ANTISPAM_ANSWER);
                saveOptionString(Options.OPTION_ANTISPAM_HELLO);
                saveOptionString(Options.OPTION_ANTISPAM_KEYWORDS);
                saveOptionBoolean(Options.OPTION_ANTISPAM_ENABLE);
                break;

            case OPTIONS_ABSENCE:
                Options.setInt(Options.OPTION_AA_TIME, form.getSelectorValue(Options.OPTION_AA_TIME) * 5);
                break;
        }
        Options.safeSave();
    }
    
    public void formAction(Forms form, boolean apply) {
        if (apply)
            save(currentOptionsForm);
        form.back();
    }

    public void select(CharSequence name, int cmd) {
        currentOptionsForm = cmd;
        form = new Forms(SawimApplication.getContext().getString(R.string.options), this);
        form.setBackPressedListener(new Forms.OnBackPressed() {
            @Override
            public boolean back() {
                save(currentOptionsForm);
                form.destroy();
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
                if (JLocale.langAvailable.length > 1) {
                    int cur = 0;
                    String curLang = Options.getString(Options.OPTION_UI_LANGUAGE);
                    for (int j = 0; j < JLocale.langAvailable.length; ++j) {
                        if (JLocale.langAvailable[j].equals(curLang)) {
                            cur = j;
                        }
                    }
                    form.addSelector(Options.OPTION_UI_LANGUAGE, "language", JLocale.langAvailableName, cur);
                }
                createSelector("fonts",
                        "10sp" + "|" + "fonts_smallest" + "|" + "fonts_small" + "|" + "fonts_normal" + "|" + "fonts_large",
                        Options.OPTION_FONT_SCHEME);
                //loadOptionInt(Options.OPTION_MIN_ITEM_SIZE, "item_height_multiplier", minItems, minItemMultipliers);

                form.addString("contact_list", null);
                setChecked("show_user_groups", Options.OPTION_USER_GROUPS);
                setChecked_(SawimApplication.getContext().getString(R.string.hide_icons_clients), Options.OPTION_HIDE_ICONS_CLIENTS);
                
                //createSelector("show_user_accounts",
                //        "no" + "|" + "by_groups" + "|" + "by_windows", Options.OPTION_USER_ACCOUNTS);
                
                //setChecked("hide_offline", Options.OPTION_CL_HIDE_OFFLINE);
                setChecked("save_temp_contacts", Options.OPTION_SAVE_TEMP_CONTACT);
                setChecked("show_status_line", Options.OPTION_SHOW_STATUS_LINE);
                setChecked("contacts_with_msg_at_top", Options.OPTION_SORT_UP_WITH_MSG);

                createSelector("sort_by",
                        "sort_by_status" + "|" + "sort_by_online" + "|" + "sort_by_name",
                        Options.OPTION_CL_SORT_BY);

                form.addString("chat", null);
				setChecked("show_platform", Options.OPTION_SHOW_PLATFORM);
                setChecked("use_history", Options.OPTION_HISTORY);
                loadOptionInt(Options.OPTION_MAX_MSG_COUNT, "max_message_count", "10|50|100|250|500|1000");
				//createSelector("presense_fonts",
                //        "10sp" + "|" + "fonts_smallest" + "|" + "fonts_small" + "|" + "fonts_normal" + "|" + "fonts_large",
                //        Options.OPTION_CHAT_PRESENSEFONT_SCHEME);
				setChecked("title_in_conference",  Options.OPTION_TITLE_IN_CONFERENCE);
				loadOptionString(Options.UNAVAILABLE_NESSAGE, "post_outputs");
                setChecked("use_simple_input", Options.OPTION_SIMPLE_INPUT);
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

            case OPTIONS_ABSENCE:
                form.addSelector(Options.OPTION_AA_TIME, "after_time", "off" + "|5 |10 |15 ", Options.getInt(Options.OPTION_AA_TIME) / 5);
                break;

      		case OPTIONS_ANSWERER:
				Answerer.getInstance().activate();
				return;
        }
        form.setControlStateListener(this);
        form.show();
    }

    public void controlStateChanged(int id) {
        switch (id) {
            case Options.OPTION_COLOR_SCHEME:
                //saveOptionSelector(Options.OPTION_COLOR_SCHEME);
                //Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
                //Scheme.updateUI();
                break;
            case Options.OPTION_FONT_SCHEME:
                saveOptionSelector(Options.OPTION_FONT_SCHEME);
                //GraphicsEx.setFontScheme(Options.getInt(Options.OPTION_FONT_SCHEME));
                //Scheme.updateUI();
                break;
			case Options.OPTION_CHAT_PRESENSEFONT_SCHEME:
				//saveOptionSelector(Options.OPTION_CHAT_PRESENSEFONT_SCHEME);
                //GraphicsEx.setChatPresenseFont(Options.getInt(Options.OPTION_CHAT_PRESENSEFONT_SCHEME));
				//Scheme.updateUI();
                break;
            case Options.OPTION_MIN_ITEM_SIZE:
                //saveOptionInt(Options.OPTION_MIN_ITEM_SIZE, minItemMultipliers);
                //Scheme.updateUI();
                break;
        }
    }
}