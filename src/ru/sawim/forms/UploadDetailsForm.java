package ru.sawim.forms;

import android.app.AlertDialog;
import android.content.DialogInterface;
import ru.sawim.R;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.modules.FileTransfer;
import java.util.Map;

public class UploadDetailsForm {

    private int ids;
    private String filename;
    private String path;
    private String size;
    private String to;
    private String time;

    public UploadDetailsForm(int id) {
        int i = 0;
        while (FileTransfer.fileMap.size() > i) {
        if (FileTransfer.fileMap.get(i).get("ID").equalsIgnoreCase(String.valueOf(id))) {
            Map<String,String> fileMap = FileTransfer.fileMap.get(i);
            this.filename = fileMap.get("FILENAME");
            this.path = fileMap.get("PATH");
            this.size = fileMap.get("FILESIZE");
            this.size = StringConvertor.bytesToSizeString(Integer.parseInt(size), false);
            this.to = fileMap.get("TO");
            this.time = fileMap.get("START");
            this.ids = i;
        }
        i++;
        }
    }

    public void show(BaseActivity activity) {
        if (filename != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setCancelable(true);
            builder.setTitle(R.string.sending_file);
            builder.setMessage(
                            JLocale.getString(R.string.path) + ": " + path +
                            "\n" + JLocale.getString(R.string.size) + ": " + size +
                            "\n" + JLocale.getString(R.string.chat) + ": " + to +
                            "\n" + JLocale.getString(R.string.upload_time) + ": " + time +
                            "\n" + JLocale.getString(R.string.status) + ": " + (!FileTransfer.canceled ? JLocale.getString(R.string.sending_file) : JLocale.getString(R.string.stopped))
                             );
            builder.setNegativeButton(JLocale.getString(R.string.cancel_upload),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int id) {
                            FileTransfer.cancelUpload(ids);
                            dialog.cancel();
                        }
                    });
            builder.setPositiveButton(JLocale.getString(R.string.close),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int id) {
                            dialog.cancel();
                        }
                    });
            builder.create().show();
        }
    }
}