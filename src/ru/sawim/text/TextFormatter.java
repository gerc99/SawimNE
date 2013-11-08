package ru.sawim.text;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.Patterns;
import protocol.Contact;
import ru.sawim.SawimApplication;
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
    private static final Emotions smiles = Emotions.instance;
    private static TextFormatter instance;

    public static TextFormatter getInstance() {
        if (instance == null)
            instance = new TextFormatter();
        return instance;
    }

    private static final int linkColor = SawimApplication.getContext().getTheme().obtainStyledAttributes(new int[] {
            android.R.attr.textColorLink,
    }).getColor(0, -1);

    public SpannableStringBuilder parsedText(final Contact contact, final String text) {
        final SpannableStringBuilder parsedText = new SpannableStringBuilder(text);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (contact.getUserId().equals(JuickMenu.JUICK) || contact.getUserId().equals(JuickMenu.JUBO))
                    getTextWithLinks(parsedText, linkColor, JuickMenu.MODE_JUICK);
                else if (contact.getUserId().equals(JuickMenu.PSTO))
                    getTextWithLinks(parsedText, linkColor, JuickMenu.MODE_PSTO);
                else
                    getTextWithLinks(parsedText, linkColor, -1);
                detectEmotions(parsedText);
            }
        }).start();
        return parsedText;
    }

    public static void detectEmotions(SpannableStringBuilder builder) {
        for (int index = 0; index < builder.length(); index++) {
            int smileIndex = smiles.getSmileChars().indexOf(builder.charAt(index));
            while (-1 != smileIndex) {
                if (builder.toString().startsWith(smiles.getSmileText(smileIndex), index)
                        && builder.toString().startsWith(" ", index - 1)) {
                    int length = smiles.getSmileText(smileIndex).length();
                    if (!isThereLinks(builder, new int[]{index, index + length})) {
                        ImageSpan imageSpan = new ImageSpan(smiles.getSmileIcon(smileIndex).getImage());
                        builder.setSpan(new ForegroundColorSpan(Color.BLUE), index, index + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.setSpan(imageSpan, index, index + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    index += length - 1;
                }
                smileIndex = smiles.getSmileChars().indexOf(builder.charAt(index), smileIndex + 1);
            }
        }
    }

    private static boolean isThereLinks(Spannable spannable, int ... positions) {
        InternalURLSpan[] spans = spannable.getSpans(0, spannable.length(), InternalURLSpan.class);
        for (int i = 0; i < spans.length; ++i) {
            ClickableSpan span = spans[i];
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
        for (Hyperlink link : msgList) {
            ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(linkColor), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        for (Hyperlink link : linkList) {
            ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(linkColor), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private final void addLinks(ArrayList<Hyperlink> links, Spannable s, Pattern pattern) {
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            Hyperlink spec = new Hyperlink();
            spec.textSpan = m.group(0);
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