package ru.sawim.text;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Patterns;
import protocol.Contact;
import protocol.xmpp.Jid;
import ru.sawim.Scheme;
import ru.sawim.view.menu.JuickMenu;
import sawim.modules.Emotions;

import java.util.ArrayList;
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


    public CharSequence parsedText(final Contact contact, final CharSequence text) {
        final int linkColor = Scheme.getColor(Scheme.THEME_LINKS);
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        if (contact != null)
            if (contact.getUserId().equals(JuickMenu.JUICK) || contact.getUserId().equals(JuickMenu.JUBO))
                getTextWithLinks(builder, linkColor, JuickMenu.MODE_JUICK);
            else if (contact.getUserId().equals(JuickMenu.PSTO))
                getTextWithLinks(builder, linkColor, JuickMenu.MODE_PSTO);
        getTextWithLinks(builder, linkColor, -1);
        detectEmotions(text, builder);
        return builder;
    }

    public CharSequence detectEmotions(CharSequence text, SpannableStringBuilder builder) {
        Matcher matcher = smilesPattern.matcher(text);
        while (matcher.find()) {
            //if (!isThereLinks(builder, new int[]{matcher.start(), matcher.end()})) {
                builder.setSpan(new ImageSpan(smiles.getSmileIcon(smiles.buildSmileyToId().get(matcher.group())).getImage()),
                        matcher.start(), matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //}
        }
        return builder;
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

    public void getTextWithLinks(final SpannableStringBuilder ssb, final int linkColor, final int mode) {
        ArrayList<Hyperlink> msgList = new ArrayList<Hyperlink>();
        ArrayList<Hyperlink> linkList = new ArrayList<Hyperlink>();
        if (mode == JuickMenu.MODE_JUICK)
            addLinks(msgList, ssb, juickPattern);
        else if (mode == JuickMenu.MODE_PSTO)
            addLinks(msgList, ssb, pstoPattern);
        addLinks(linkList, ssb, Patterns.WEB_URL);
        addLinks(linkList, ssb, Jid.pattern);
        //addLinks(linkList, ssb, Patterns.EMAIL_ADDRESS);
        for (Hyperlink link : msgList) {
            ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //ssb.setSpan(new ForegroundColorSpan(linkColor), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        for (Hyperlink link : linkList) {
            ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //ssb.setSpan(new ForegroundColorSpan(linkColor), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private final void addLinks(ArrayList<Hyperlink> links, Spannable s, Pattern pattern) {
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            Hyperlink spec = new Hyperlink();
            spec.textSpan = s.subSequence(start, end).toString();
            spec.span = new InternalURLSpan(spec.textSpan.toString());
            spec.start = start;
            spec.end = end;
            links.add(spec);
        }
    }

    class Hyperlink {
        String textSpan;
        InternalURLSpan span;
        int start;
        int end;
    }
}