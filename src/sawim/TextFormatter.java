package sawim;

import DrawControls.icons.Icon;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import sawim.modules.Emotions;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 19.06.13
 * Time: 20:45
 * To change this template use File | Settings | File Templates.
 */
public class TextFormatter {

    private static void detectEmotions(Context context,
                                SpannableStringBuilder builder, int startPos, int endPos) {
        Emotions smiles = Emotions.instance;
        for (int index = startPos; index < endPos; ++index) {
            int smileIndex = smiles.getSmile(builder.toString(), index);
            if (-1 != smileIndex) {
                int length = smiles.getSmileText(smileIndex).length();
                Icon icon = smiles.getSmileIcon(smileIndex);
                Bitmap bitmap = Bitmap.createBitmap(icon.getImage().getBitmap(), icon.x, icon.y, icon.getWidth(), icon.getHeight());
                ImageSpan imageSpan = new ImageSpan(context, bitmap, ImageSpan.ALIGN_BASELINE);
                builder.setSpan(imageSpan, index, index + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                index += length - 1;
                break;
            }
        }
    }

    public static Spannable getFormattedText(String text, Context context, int color) {
        return getFormattedText(new SpannableStringBuilder(text), context, color);
    }

    public static Spannable getFormattedText(SpannableStringBuilder builder, Context context, int color) {
        if(builder == null) {
            builder = new SpannableStringBuilder("");
        }
        String text = builder.toString();
        detectEmotions(context, builder, 0, text.length());
        builder.setSpan(new ForegroundColorSpan(color), 0, text.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
}
