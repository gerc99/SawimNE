
package sawim.modules.fs;

import sawim.SawimException;
import sawim.comm.Util;

import java.util.Vector;


public final class FileBrowser implements Runnable {
    private static final int TYPE_FILE = 1;
    private static final int TYPE_DIR = 0;
    private static final int TYPE_PARENT_DIR = 0;
    private static final int TYPE_DISK = 0;

    private FileBrowserListener listener;
    private boolean needToSelectDirectory;
    private boolean selectFirst;

    private String currDir;

    private String nextDir = null;
    private Vector root = new Vector();

    //private Par errorMessage = null;

    private void setError(SawimException err) {
        /*VirtualListItem parser = new List<VirtualListItem>(getFontSet(), getWidth() * 8 / 10);
        parser.addDescription(err.getMessage(), THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        errorMessage = parser.getPar();
        currDir = FileSystem.ROOT_DIRECTORY;
        root = new Vector();
        restoring();
        invalidate();*/
    }

    public FileBrowser(boolean selectDir) {
        //super(selectDir ? "Dirs" : "Files");
        needToSelectDirectory = selectDir;
    }

    public void setListener(FileBrowserListener _listener) {
        this.listener = _listener;
    }

    public void activate() {
        if (sawim.modules.fs.FileSystem.isSupported()) {
            rebuildTree(FileSystem.ROOT_DIRECTORY);
            //show();
        }
    }

    private FileNode createParentDir(String file) {
        int i = file.lastIndexOf('/', file.length() - 2);
        if (i <= 0) {
            return null;
        }
        return new FileNode(file.substring(0, i + 1), file.substring(i + 1));
    }

    public void run() {
        selectFirst = false;
        try {
            String currentPath = nextDir;
            JSR75FileSystem fs = FileSystem.getInstance();
            Vector newRoot = fs.getDirectoryContents(currentPath, needToSelectDirectory);

            Vector files = new Vector();
            for (int i = 0; i < newRoot.size(); ++i) {
                FileNode file = (FileNode) newRoot.elementAt(i);
                if (!FileSystem.PARENT_DIRECTORY.equals(file.getText())) {
                    files.addElement(file);
                }
            }
            Util.sort(files);
            if (needToSelectDirectory) {
                FileNode parent = createParentDir(currentPath);
                if (null != parent) {
                    files.insertElementAt(parent, 0);
                    selectFirst = true;
                }
            }
            //lock();
            //setAllToTop();
            root = files;
            currDir = currentPath;
            restoring();
            //unlock();

        } catch (SawimException e) {
            setError(e);

        } catch (Exception e) {
            setError(new SawimException(191, 2));
        }
        nextDir = null;
    }

    private void rebuildTree(String next) {
        if (null == nextDir) {
            nextDir = next;
            new Thread(this).start();
        }
    }

    private void fileNodeSelected() {
        FileNode file = getCurrentFile();
        if (null == file) {
            return;
        }
        String fullpath = file.getFullName();
        //if (selectFirst && (0 == getCurrItem())) {
        //    listener.onDirectorySelect(fullpath);
        //   return;
        //}

        if (file.isDir()) {
            rebuildTree(fullpath);

        } else {
            try {
                listener.onFileSelect(fullpath);
            } catch (SawimException e) {
                setError(e);
            }
        }
    }

    protected void doSawimAction(int keyCode) {
        switch (keyCode) {
            /*case NativeCanvas.Sawim_SELECT:
                fileNodeSelected();
                return;

            case NativeCanvas.Sawim_BACK:
                if ((null != errorMessage) || FileSystem.ROOT_DIRECTORY.equals(currDir)) {
                    RosterHelper.getSawimActivity().updateRoster();

                } else {
                    int d = currDir.lastIndexOf('/', currDir.length() - 2);
                    rebuildTree(currDir.substring(0, d + 1));
                }
                return;*/
        }
    }

    protected boolean hasMenu() {
        return false;
    }

    private FileNode getCurrentFile() {
        /*int num = getCurrItem();
        if ((0 <= num) && (num < root.size())) {
            return (FileNode)root.elementAt(num);
        }*/
        return null;
    }

    protected void restoring() {
        String cmd = "open";
        FileNode file = getCurrentFile();
        /*if (null != file) {
            if (selectFirst ? (0 == getCurrItem()) : file.isFile()) {
                cmd = "onContextItemSelected";
            }
        }
        if (null != errorMessage) {
            cmd = "";
        }
        setSoftBarLabels(cmd, cmd, "back", false); */
    }

    protected int getSize() {
        return root.size();
    }

   /* protected void drawEmptyItems(GraphicsEx g, int top_y) {
        if (null != errorMessage) {
            int height = getHeight() - top_y;
            errorMessage.paint(getFontSet(), g, getWidth() / 10,
                    top_y + (height - errorMessage.getHeight()) / 2,
                    0, errorMessage.getHeight());
        }
    }
    protected void drawItemData(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to) {
        if (selectFirst && (0 == index)) {
            g.setThemeColor(Scheme.THEME_CAP_BACKGROUND);
            g.fillRect(x1, y1, w, h);
            g.setThemeColor(Scheme.THEME_CAP_TEXT);
        }else {
            g.setThemeColor(THEME_TEXT);
        }
        g.setFont(getDefaultFont());
        FileNode node = (FileNode)root.elementAt(index);
        boolean isDir = node.isDir() || node.isParentDir();
        g.drawString(getIcon(isDir ? TYPE_DIR : TYPE_FILE), node.getDescStr(), null,
                x1, y1, w, h);
        if (selectFirst && (0 == index)) {
            g.drawLine(x1, y1 + h, x1 + w, y1 + h);
        }
    }*/
}


