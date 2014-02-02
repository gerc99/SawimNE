package sawim.history;

import android.util.Log;
import protocol.Contact;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.config.HomeDirectory;

import sawim.SawimException;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.modules.Notify;
import sawim.modules.fs.FileSystem;
import sawim.modules.fs.JSR75FileSystem;
import sawim.roster.RosterHelper;
import sawim.util.JLocale;

import java.io.IOException;
import java.io.OutputStream;


class HistoryExport implements Runnable {
    private HistoryStorage exportHistory;

    private HistoryStorageList screen;
    private JSR75FileSystem file;
    String contact;
    int currentMessage;
    int messageCount;

    public HistoryExport(HistoryStorageList screen) {
        this.screen = screen;
    }

    public void export(HistoryStorage storage) {
        exportHistory = storage;
        new Thread(this).start();
    }

    private void setProgress(int messageNum) {
        currentMessage = messageNum;
        //screen.invalidate();
    }

    public void run() {
        try {
            exportContact(exportHistory);
            Notify.getSound().playSoundNotification(Notify.NOTIFY_MESSAGE);
            screen.exportDone();
            RosterHelper.getInstance().activateWithMsg(SawimApplication.getContext().getString(R.string.saved_in) + " " + getFile(exportHistory).getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            SawimException e = new SawimException(191, 5);
            if (ex instanceof SawimException) {
                e = (SawimException) ex;
            }
            RosterHelper.getInstance().activateWithMsg(e.getMessage());
        }
    }

    private void write(OutputStream os, String val) throws IOException {
        os.write(StringConvertor.stringToByteArrayUtf8(val));
    }

    private void exportUinToStream(HistoryStorage storage, OutputStream os) throws IOException {
        messageCount = storage.getHistorySize();
        if (0 == messageCount) {
            return;
        }
        os.write(new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf});

        Contact c = storage.getContact();
        String userId = c.getUserId();
        String nick = (c.getName().length() > 0) ? c.getName() : userId;
        contact = nick;
        setProgress(0);
        write(os, "\r\n\t" + JLocale.getString("message_history_with")
                + nick + " (" + userId + ")\r\n\t"
                + JLocale.getString("export_date")
                + Util.getLocalDateString(SawimApplication.getCurrentGmtTime(), false)
                + "\r\n\r\n");

        String me = JLocale.getString("me");
        int guiStep = Math.max(messageCount / 100, 1) * 5;
        for (int i = 0, curStep = 0; i < messageCount; ++i) {
            CachedRecord record = storage.getRecord(i);
            write(os, " " + ((record.type == 0) ? (c.isConference() ? record.from : nick) : me)
                    + " (" + record.date + "):\r\n");
            write(os, StringConvertor.restoreCrLf(record.text) + "\r\n");
            curStep++;
            if (curStep > guiStep) {
                os.flush();
                setProgress(i);
                curStep = 0;
            }
        }
        setProgress(messageCount);
        os.flush();
    }

    private JSR75FileSystem getFile(HistoryStorage storage) {
        return HomeDirectory.getFile(FileSystem.HISTORY + "/" + storage.getUniqueUserId() + ".txt");
    }

    private void exportContact(HistoryStorage storage) throws Exception {
        storage.openHistory();
        try {
            if (0 < storage.getHistorySize()) {
                JSR75FileSystem file = getFile(storage);
                if (file.exists()) {
                    RosterHelper.getInstance().activateWithMsg(SawimApplication.getContext().getString(R.string.file_already_saved));
                }
                OutputStream out = file.openOutputStream();
                try {
                    exportUinToStream(storage, out);
                } finally {
                    out.close();
                    file.close();
                }
            }
        } finally {
            storage.closeHistory();
        }
    }

    private void closeFile() {
        if (null != file) {
            file.close();
            file = null;
        }
        //TcpSocket.close(fis);
        //fis = null;
    }
}



