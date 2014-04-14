


package ru.sawim.modules.fs;

import ru.sawim.SawimException;
import ru.sawim.activities.BaseActivity;

import java.io.InputStream;


public interface FileBrowserListener {
    public void onFileSelect(BaseActivity activity, InputStream in, String fileName) throws SawimException;
}


