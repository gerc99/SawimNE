package DrawControls.icons;


public class AniIcon extends Icon {
    private Icon[] frames;
    private int[] delays;

    public AniIcon(Icon icon, int frameCount) {
        super(icon.getImage());
        frames = new Icon[frameCount];
        delays = new int[frameCount];
    }

    void addFrame(int num, Icon icon, int dalay) {
        frames[num] = icon;
        delays[num] = dalay;
    }

    public Icon[] getImages() {
        return frames;
    }

    public int[] getDelays() {
        return delays;
    }
}