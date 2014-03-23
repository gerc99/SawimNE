


package ru.sawim.modules.fs;

import ru.sawim.SawimException;

import java.io.InputStream;


public interface FileBrowserListener {
    public void onFileSelect(InputStream in, String fileName) throws SawimException;
}


