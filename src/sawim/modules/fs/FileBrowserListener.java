


package sawim.modules.fs;

import sawim.SawimException;


public interface FileBrowserListener {
    public void onFileSelect(String file) throws SawimException;

    public void onDirectorySelect(String directory);
}


