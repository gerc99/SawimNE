package ru.sawim.io;

import android.net.Uri;
import ru.sawim.SawimException;
import ru.sawim.activities.BaseActivity;

public interface FileBrowserListener {
    void onFileSelect(BaseActivity activity, Uri fileUri) throws SawimException;
}


