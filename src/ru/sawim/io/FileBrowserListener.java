


package ru.sawim.io;

import android.net.Uri;
import ru.sawim.SawimException;
import ru.sawim.activities.BaseActivity;

import java.io.InputStream;


public interface FileBrowserListener {
    public void onFileSelect(BaseActivity activity, Uri fileUri) throws SawimException;
}


