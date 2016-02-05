package ru.sawim.text;

import android.text.*;
import android.text.style.ImageSpan;
import ru.sawim.modules.Emotions;
import ru.sawim.view.menu.JuickMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 19.06.13
 * Time: 20:45
 * To change this template use File | Settings | File Templates.
 */
public class TextFormatter {

    private Pattern juickPattern = Pattern.compile("(#[0-9]+(/[0-9]+)?)");
    private Pattern pstoPattern = Pattern.compile("(#[\\w]+(/[0-9]+)?)");
    private Pattern nickPattern = Pattern.compile("(@[\\w@.-]+(/[0-9]+)?)");
    private Pattern urlPattern = Pattern.compile("(([^ @/<>'\\\"]+)@([^ @/<>'\\\"]+)\\.([a-zA-Z\\.]{2,6})(?:/([^ <>'\\\"]*))?)|(((?:(http|https|Http|Https):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?((?:(?:[a-zA-Z0-9а-яА-Я][a-zA-Zа-яА-Я0-9\\-]{0,64}\\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:inc|info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnprwyz]|l[abcikrstuvy]|(?:mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eosuw]|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agksyz]|v[aceginu]|w[fs]|(?:рф|xxx)|y[et]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])))(?:\\:\\d{1,5})?)(\\/(?:(?:[a-zA-Zа-яА-Я0-9\\;\\/\\?\\:\\@\\&\\=\\#\\~\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?(?:\\b|$))");
    private Pattern smilesPattern;
    private static final Emotions smiles = Emotions.instance;
    private static TextFormatter instance;

    public static void init() {
        if (instance == null)
            instance = new TextFormatter();
    }

    public static TextFormatter getInstance() {
        return instance;
    }

    private TextFormatter() {
        smilesPattern = compilePattern();
    }

    public CharSequence detectEmotions(SpannableStringBuilder builder) {
        Matcher matcher = smilesPattern.matcher(builder);
        while (matcher.find()) {
            if (!isThereLinks(builder, matcher.start(), matcher.end())) {
                builder.setSpan(new ImageSpan(smiles.getSmileIcon(smiles.buildSmileyToId().get(matcher.group())).getImage()),
                        matcher.start(), matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return builder;
    }

    public CharSequence detectEmotions(CharSequence text) {
        Spannable s;
        if (text instanceof Spannable) {
            s = (Spannable) text;
        } else {
            s = Spannable.Factory.getInstance().newSpannable(text);
        }
        Matcher matcher = smilesPattern.matcher(text);
        while (matcher.find()) {
            s.setSpan(new ImageSpan(smiles.getSmileIcon(smiles.buildSmileyToId().get(matcher.group())).getImage()),
                    matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    private Pattern compilePattern() {
        StringBuilder patternString = new StringBuilder(smiles.getSmileTexts().length * 3);
        patternString.append('(');
        for (String s : smiles.getSmileTexts()) {
            patternString.append(Pattern.quote(s));
            patternString.append('|');
        }
        patternString.replace(patternString.length() - 1, patternString.length(), ")");
        return Pattern.compile(patternString.toString());
    }

    private static boolean isThereLinks(Spannable spannable, int... positions) {
        InternalURLSpan[] spans = spannable.getSpans(0, spannable.length(), InternalURLSpan.class);
        for (int i = 0; i < spans.length; ++i) {
            InternalURLSpan span = spans[i];
            int spanStart = spannable.getSpanStart(span);
            int spanEnd = spannable.getSpanEnd(span);
            int lengthPos = positions.length;

            for (int k = 0; k < lengthPos; ++k) {
                int position = positions[k];
                if (spanStart < position && position < spanEnd) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addLinksForBots(final SpannableStringBuilder ssb, final int mode) {
        switch (mode) {
            case JuickMenu.MODE_JUICK:
                addLinks(ssb, juickPattern);
                addLinks(ssb, nickPattern);
                break;
            case JuickMenu.MODE_PSTO:
                addLinks(ssb, pstoPattern);
                addLinks(ssb, nickPattern);
                break;
        }
    }

    public List<String> getLinks(final SpannableStringBuilder ssb) {
        List<String> links = new ArrayList<>();
        Matcher m = urlPattern.matcher(ssb);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            String textSpan = ssb.subSequence(start, end).toString();
            InternalURLSpan span = new InternalURLSpan(textSpan);
            ssb.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            links.add(textSpan);
        }
        return links;
    }

    private void addLinks(Spannable s, Pattern pattern) {
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            String textSpan = s.subSequence(start, end).toString();
            InternalURLSpan span = new InternalURLSpan(textSpan);
            s.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
