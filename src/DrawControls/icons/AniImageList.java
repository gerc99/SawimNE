package DrawControls.icons;

import ru.sawim.General;

import java.io.InputStream;


public class AniImageList extends ImageList {

    private AniIcon[] icons;
    private static final int WAIT_TIME = 100;

    public AniImageList() {
    }

    public Icon iconAt(int index) {
        if (index < size() && index >= 0) {
            return icons[index];
        }
        return null;
    }

    public int size() {
        return icons != null ? icons.length : 0;
    }

    private String getAnimationFile(String resName, int i) {
        return resName + "/" + (i + 1) + ".png";
    }

    public void load(String resName, int w, int h) {
        try {
            InputStream is = General.getResourceAsStream(resName + "/animate.bin");
            int smileCount = is.read() + 1;
            icons = new AniIcon[smileCount];
            ImageList imgs = new ImageList();
            for (int smileNum = 0; smileNum < smileCount; ++smileNum) {
                int imageCount = is.read();
                int frameCount = is.read();
                imgs.load(getAnimationFile(resName, smileNum), imageCount);
                boolean loaded = (0 < imgs.size());
                AniIcon icon = loaded ? new AniIcon(imgs.iconAt(0), frameCount) : null;
                for (int frameNum = 0; frameNum < frameCount; ++frameNum) {
                    int iconIndex = is.read();
                    int delay = is.read() * WAIT_TIME;
                    if (loaded) {
                        icon.addFrame(frameNum, imgs.iconAt(iconIndex), delay);
                    }
                }
                icons[smileNum] = icon;
            }
        } catch (Exception e) {
        }
    }
}