


package DrawControls.icons;



public class AniIcon extends Icon {
    private Icon[] frames;
    private int[] delays;
    private int currentFrame = 0;
    

    public AniIcon(Icon icon, int frameCount) {
        super(icon.getImage());
        frames = new Icon[frameCount];
        delays = new int[frameCount];
    }
    //public Image getImage() {
    //    return frames[currentFrame].getImage();
    //}
    public Icon[] getImages() {
        return frames;
    }
    void addFrame(int num, Icon icon, int dalay) {
        frames[num] = icon;
        delays[num] = dalay;
    }
    //public void drawByLeftTop(Graphics g, int x, int y) {
    //    frames[currentFrame].drawByLeftTop(g, x, y);
    //    painted = true;
    //}
    private boolean painted = false;
    private long sleepTime = 0;
    boolean nextFrame(long deltaTime) {
        /**sleepTime -= deltaTime;
        if (sleepTime <= 0) {
            currentFrame = (currentFrame + 1) % frames.length;
            sleepTime = delays[currentFrame];
            boolean needReepaint = painted;
            painted = false;
            return needReepaint;
        }*/
        return false;
    }
    
}


