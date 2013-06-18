

package DrawControls.icons;

public class Icon {
    private Image image;
    public int x = 0;
    public int y = 0;
	private int width = 0;
    private int height = 0;

    public Icon(Image image, int x, int y, int width, int height) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public Image getImage() {
        return image;
    }

	
	public int getWidth() {
		return width;
	}

	
	public int getHeight() {
		return height;
	}

    /*public void drawByLeftTop(Graphics g, int x, int y) {
        if (getImage() == null) {
            return;
        }
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        int iy = y - this.y;
        int ix = x - this.x;
        g.clipRect(x, y, width, height);
        g.drawImage(getImage(), ix, iy, Graphics.TOP | Graphics.LEFT);
        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    
    public void drawInCenter(Graphics g, int x, int y) {
        drawByLeftTop(g, x - width / 2, y - height / 2);
    }*/
}

