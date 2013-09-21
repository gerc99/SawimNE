package ru.sawim.text;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Patterns;
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
    static Emotions smiles = Emotions.instance;

    public static SpannableStringBuilder detectEmotions(String message) {
        SpannableStringBuilder builder = new SpannableStringBuilder(message);
        for (int index = 0; index < message.length(); ++index) {
            int smileIndex = smiles.getSmileChars().indexOf(message.charAt(index));
            while (-1 != smileIndex) {
                if (message.startsWith(smiles.getSmileText(smileIndex), index)) {
                    int length = smiles.getSmileText(smileIndex).length();
                    builder.setSpan(new ImageSpan(smiles.getSmileIcon(smileIndex).getImage()), index, index + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    index += length - 1;
                }
                smileIndex = smiles.getSmileChars().indexOf(message.charAt(index), smileIndex + 1);
            }
        }
        return builder;
    }

    public SpannableStringBuilder getTextWithLinks(SpannableStringBuilder ssb, int linkColor, JuickMenu.Mode mode) {
        ArrayList<Hyperlink> msgList = new ArrayList<Hyperlink>();
        ArrayList<Hyperlink> linkList = new ArrayList<Hyperlink>();
        if (mode == JuickMenu.Mode.juick)
            addLinks(msgList, ssb, juickPattern);
        else if (mode == JuickMenu.Mode.psto)
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
        return ssb;
    }

    private final void addLinks(ArrayList<Hyperlink> links, Spannable s, Pattern pattern) {
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            Hyperlink spec = new Hyperlink();
            spec.textSpan = s.subSequence(start, end);
            spec.span = new InternalURLSpan(spec.textSpan.toString());
            spec.start = start;
            spec.end = end;
            links.add(spec);
        }
    }

    class Hyperlink {
        CharSequence textSpan;
        InternalURLSpan span;
        int start;
        int end;
    }
}