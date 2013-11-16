package sawim.modules.fs;

import sawim.roster.TreeNode;

public class FileNode extends TreeNode {
    final private String path;
    final private String filename;
    private long size = -1;

    public FileNode(String path, String filename) {
        this.path = path;
        this.filename = filename;
    }

    @Override
    public byte getType() {
        return 0;
    }

    public String getText() {
        return filename;
    }

    public int getNodeWeight() {
        if (isParentDir()) {
            return 0;
        }
        if (isDir()) {
            return 10;
        }
        return 20;
    }

    public boolean isDir() {
        return filename.endsWith("/");
    }

    public boolean isParentDir() {
        return filename.equals(FileSystem.PARENT_DIRECTORY);
    }

    public boolean isDisk() {
        return path.equals(FileSystem.ROOT_DIRECTORY);
    }

    public boolean isFile() {
        return !isDir();
    }

    public long size() {
        if (!isDisk() && !isFile()) {
            return 0;
        }
        if (-1 == size) {
            try {
                JSR75FileSystem fs = FileSystem.getInstance();
                fs.openFile(getFullName());
                size = isFile() ? fs.fileSize() : fs.totalSize();
                fs.close();
            } catch (Exception e) {
                return 0;
            }
        }
        return size;
    }

    public String getFullName() {
        if (isParentDir()) {
            int d = path.lastIndexOf('/', path.length() - 2);
            return (d != -1) ? path.substring(0, d + 1) : FileSystem.ROOT_DIRECTORY;
        }
        return path + filename;
    }
}


