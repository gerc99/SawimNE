package ru.sawim.text;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Patterns;
import protocol.Contact;
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
    private static final TextFormatter instance = new TextFormatter();

    public static TextFormatter getInstance() {
        return instance;
    }

    public SpannableStringBuilder parsedText(final Contact contact, final String text) {
        final SpannableStringBuilder parsedText = new SpannableStringBuilder(text);
        new Thread(new Runnable() {
            @Override
            public void run() {
                TextFormatter.detectEmotions(parsedText, text);
                if (contact.getUserId().equals(JuickMenu.JUICK) || contact.getUserId().equals(JuickMenu.JUBO))
                    getTextWithLinks(parsedText, 0xff00e4ff, JuickMenu.Mode.juick);
                else if (contact.getUserId().equals(JuickMenu.PSTO))
                    getTextWithLinks(parsedText, 0xff00e4ff, JuickMenu.Mode.psto);
                else
                    getTextWithLinks(parsedText, 0xff00e4ff, JuickMenu.Mode.none);
            }
        }).start();
        return parsedText;
    }

    public static void detectEmotions(SpannableStringBuilder builder, String message) {
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
    }

    private void getTextWithLinks(final SpannableStringBuilder ssb, final int linkColor, final JuickMenu.Mode mode) {
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