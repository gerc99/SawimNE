package sawim;

import DrawControls.icons.Icon;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.text.util.Linkify;
import sawim.modules.Emotions;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 19.06.13
 * Time: 20:45
 * To change this template use File | Settings | File Templates.
 */
public class TextFormatter {
    static Emotions smiles = Emotions.instance;

    private static SpannableStringBuilder detectEmotions(Context context,
                                                         SpannableStringBuilder builder) {
        String message = builder.toString();
        for (int index = 0; index < message.length(); ++index) {
            int smileIndex = smiles.getSmileChars().indexOf(message.charAt(index));
            while (-1 != smileIndex) {
                if (message.startsWith(smiles.getSmileText(smileIndex), index)) {
                    int length = smiles.getSmileText(smileIndex).length();
                    Icon icon = smiles.getSmileIcon(smileIndex);
                    builder.setSpan(new ImageSpan(context,
                            Bitmap.createBitmap(icon.getImage().getBitmap(), icon.x, icon.y, icon.getWidth(), icon.getHeight()),
                            ImageSpan.ALIGN_BASELINE), index, index + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    index += length - 1;
                }
                smileIndex = smiles.getSmileChars().indexOf(message.charAt(index), smileIndex + 1);
            }
        }
        return builder;
    }

    public static SpannableStringBuilder getFormattedText(String text, Context context) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        //if (Linkify.addLinks(builder, 1)) return builder;
        return detectEmotions(context, builder);
    }
}