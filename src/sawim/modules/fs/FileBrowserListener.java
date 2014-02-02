


package sawim.modules.fs;

import sawim.SawimException;

import java.io.InputStream;


public interface FileBrowserListener {
    public void onFileSelect(InputStream in, String fileName) throws SawimException;
}


