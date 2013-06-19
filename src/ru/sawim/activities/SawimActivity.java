/**
 *  MicroEmulator
 *  Copyright (C) 2008 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 *
 */

package ru.sawim.activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.*;
import sawim.FileTransfer;
import sawim.Sawim;
import sawim.Options;
import sawim.OptionsForm;
import sawim.chat.ChatHistory;
import sawim.cl.ContactList;
import sawim.forms.ManageContactListForm;
import sawim.history.HistoryStorage;
import sawim.modules.Notify;
import sawim.modules.photo.PhotoListener;
import org.microemu.MIDletBridge;
import org.microemu.cldc.file.FileSystem;
import org.microemu.util.AndroidLoggerAppender;
import org.microemu.app.Common;
import org.microemu.log.Logger;
import org.microemu.util.AndroidRecordStoreManager;
import protocol.Protocol;
import protocol.StatusInfo;
import protocol.icq.Icq;
import protocol.jabber.Jabber;
import protocol.mrim.Mrim;
import ru.sawim.*;
import ru.sawim.photo.CameraActivity;
import ru.sawim.view.StatusesView;
import ru.sawim.view.XStatusesView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class SawimActivity extends FragmentActivity {

    public static final String LOG_TAG = "SawimActivity";
    public static String PACKAGE_NAME = null;

    public Common common;
    private static SawimActivity instance;
    private NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();

    public static SawimActivity getInstance() {
        return instance;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        new General().init(this);
        setContentView(R.layout.main);
        instance = this;
        PACKAGE_NAME = getApplicationContext().getPackageName();

        Logger.removeAllAppenders();
        Logger.setLocationEnabled(false);
        Logger.addAppender(new AndroidLoggerAppender());

        System.setOut(new PrintStream(new OutputStream() {
            StringBuffer line = new StringBuffer();

            @Override
            public void write(int oneByte) throws IOException {
                if (((char) oneByte) == '\n') {
                    Logger.debug(line.toString());
                    if (line.length() > 0) {
                        line.delete(0, line.length() - 1);
                    }
                } else {
                    line.append((char) oneByte);
                }
            }
        }));
        System.setErr(new PrintStream(new OutputStream() {
            StringBuffer line = new StringBuffer();

            @Override
            public void write(int oneByte) throws IOException {
                if (((char) oneByte) == '\n') {
                    Logger.debug(line.toString());
                    if (line.length() > 0) {
                        line.delete(0, line.length() - 1);
                    }
                } else {
                    line.append((char) oneByte);
                }
            }
        }));
        MIDletInit();
        if (!General.initialized) {
            new Sawim().startApp();
            ChatHistory.instance.loadUnreadMessages();
            updateAppIcon();
            ContactList.getInstance().autoConnect();
        }
    }

    private void MIDletInit() {
        common = new Common();
        MIDletBridge.setMicroEmulator(common);
        common.setRecordStoreManager(new AndroidRecordStoreManager(this));
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            System.setProperty("video.snapshot.encodings", "yes");
        }
        FileSystem fs = new FileSystem();
        fs.registerImplementation();

        startService();
        networkStateReceiver.updateNetworkState(this);
        common.initMIDlet();
    }

    private void startService() {
        startService(new Intent(this, SawimService.class));
        registerReceiver(networkStateReceiver, networkStateReceiver.getFilter());
        bindService(new Intent(this, SawimService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        quit();
        super.onDestroy();
    }

    private void quit() {
        unbindService(serviceConnection);
        unregisterReceiver(networkStateReceiver);
        stopService(new Intent(this, SawimService.class));
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
    }

    @Override
    public void onBackPressed() {
        minimizeApp();
    }

    public void recreateActivity() {
        finish();
        startActivity(new Intent(this, SawimActivity.class));
    }

    private static final int MENU_CONNECT = 0;
    private static final int MENU_STATUS = 1;
    private static final int MENU_XSTATUS = 2;
    private static final int MENU_PRIVATE_STATUS = 3;
    private static final int MENU_SOUND = 4;
    private static final int MENU_OPTIONS = 5;
    private static final int MENU_OPTIONS_THEMES = 6;
    private static final int MENU_QUIT = 7;//OptionsForm
    private static final int MENU_MORE = 14;
    private static final int MENU_DISCO = 15;
    private static final int MENU_NOTES = 16;
    private static final int MENU_GROUPS = 17;
    private static final int MENU_MYSELF = 18;
    private static final int MENU_MICROBLOG = 19;//ManageContactListForm
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        Protocol p = ContactList.getInstance().getCurrProtocol();
        menu.add(Menu.NONE, MENU_CONNECT, Menu.NONE, R.string.connect);
        if (p != null)
            menu.findItem(MENU_CONNECT).setTitle((p.isConnected() || p.isConnecting()) ? R.string.disconnect : R.string.connect);
        menu.add(Menu.NONE, MENU_STATUS, Menu.NONE, R.string.status);
        menu.add(Menu.NONE, MENU_XSTATUS, Menu.NONE, R.string.xstatus);
        if (p != null) {
            if ((p instanceof Icq) || (p instanceof Mrim))
                menu.add(Menu.NONE, MENU_PRIVATE_STATUS, Menu.NONE, R.string.private_status);
        }
        if (p != null)
            if (p.isConnected()) {
                SubMenu moreMenu = menu.addSubMenu(Menu.NONE, MENU_MORE, Menu.NONE, R.string.more);
                if (p instanceof Jabber) {
                    moreMenu.add(Menu.NONE, MENU_DISCO, Menu.NONE, R.string.service_discovery);
                    moreMenu.add(Menu.NONE, MENU_NOTES, Menu.NONE, R.string.notes);
                }
                moreMenu.add(Menu.NONE, MENU_GROUPS, Menu.NONE, R.string.manage_contact_list);
                if (p.hasVCardEditor())
                    moreMenu.add(Menu.NONE, MENU_MYSELF, Menu.NONE, R.string.myself);
                if (p instanceof Mrim)
                    moreMenu.add(Menu.NONE, MENU_MICROBLOG, Menu.NONE, R.string.microblog);
            }
        menu.add(Menu.NONE, MENU_SOUND, Menu.NONE, Options.getBoolean(Options.OPTION_SILENT_MODE)
                ? R.string.sound_on : R.string.sound_off);
        SubMenu optionsMenu = menu.addSubMenu(Menu.NONE, MENU_OPTIONS, Menu.NONE, R.string.options);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ACCOUNT, Menu.NONE, R.string.options_account);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_INTERFACE, Menu.NONE, R.string.options_interface);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_SIGNALING, Menu.NONE, R.string.options_signaling);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ANTISPAM, Menu.NONE, R.string.antispam);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ABSENCE, Menu.NONE, R.string.absence);
        optionsMenu.add(Menu.NONE, OptionsForm.OPTIONS_ANSWERER, Menu.NONE, R.string.answerer);

        menu.add(Menu.NONE, MENU_QUIT, Menu.NONE, R.string.quit);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Protocol p = ContactList.getInstance().getCurrProtocol();
        switch (item.getItemId()) {
            case MENU_CONNECT:
                if (p.isConnected()) {
                    p.setStatus(StatusInfo.STATUS_OFFLINE, "");
                } else {
                    p.setStatus(StatusInfo.STATUS_ONLINE, "");
                }
                break;
            case MENU_STATUS:
                new StatusesView(StatusesView.ADAPTER_STATUS).show(getSupportFragmentManager(), "change-status");
                break;
            case MENU_XSTATUS:
                new XStatusesView().show(getSupportFragmentManager(), "change-xstatus");
                break;
            case MENU_PRIVATE_STATUS:
                new StatusesView(StatusesView.ADAPTER_PRIVATESTATUS).show(getSupportFragmentManager(), "change-private-status");
                break;
            case MENU_SOUND:
                Notify.getSound().changeSoundMode(false);
                break;

            case MENU_DISCO:
                ((Jabber)p).getServiceDiscovery().showIt();
                break;
            case MENU_NOTES:
                ((Jabber)p).getMirandaNotes().showIt();
                break;
            case MENU_GROUPS:
                new ManageContactListForm(p).showMenu(this);
                break;
            case MENU_MYSELF:
                p.showUserInfo(p.createTempContact(p.getUserId(), p.getNick()));
                break;
            case MENU_MICROBLOG:
                ((Mrim)p).getMicroBlog().activate();
                break;

            case OptionsForm.OPTIONS_ACCOUNT:
                startActivity(new Intent(this, AccountsListActivity.class));
                break;
            case OptionsForm.OPTIONS_INTERFACE:
                new OptionsForm().select(OptionsForm.OPTIONS_INTERFACE);
                break;
            case OptionsForm.OPTIONS_SIGNALING:
                new OptionsForm().select(OptionsForm.OPTIONS_SIGNALING);
                break;
            case OptionsForm.OPTIONS_ANTISPAM:
                new OptionsForm().select(OptionsForm.OPTIONS_ANTISPAM);
                break;
            case OptionsForm.OPTIONS_ABSENCE:
                new OptionsForm().select(OptionsForm.OPTIONS_ABSENCE);
                break;
            case OptionsForm.OPTIONS_ANSWERER:
                new OptionsForm().select(OptionsForm.OPTIONS_ANSWERER);
                break;

            case MENU_QUIT:
                Sawim.getSawim().quit();
                quit();
                finish();
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    public boolean isNetworkAvailable() {
        return networkStateReceiver.isNetworkAvailable();
    }

    private PhotoListener photoListener = null;
    private FileTransfer fileTransferListener = null;
    private static final int RESULT_PHOTO = RESULT_FIRST_USER + 1;
    private static final int RESULT_EXTERNAL_PHOTO = RESULT_FIRST_USER + 2;
    private static final int RESULT_EXTERNAL_FILE = RESULT_FIRST_USER + 3;

    public void startCamera(PhotoListener listener, int width, int height) {
        photoListener = listener;
        if (1000 < Math.max(width, height)) {
            try {
                Intent extCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (!isCallable(extCameraIntent)) throw new Exception("not found");
                startActivityForResult(extCameraIntent, RESULT_EXTERNAL_PHOTO);
                return;
            } catch (Exception ignored) {
            }
        }
        Intent cameraIntent = new Intent(this, CameraActivity.class);
        cameraIntent.putExtra("width", width);
        cameraIntent.putExtra("height", height);
        startActivityForResult(cameraIntent, RESULT_PHOTO);
    }

    public boolean pickFile(FileTransfer listener) {
        try {
            fileTransferListener = listener;
            Intent theIntent = new Intent(Intent.ACTION_GET_CONTENT);
            theIntent.setType("file/*");
            theIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            if (!isCallable(theIntent)) return false;
            startActivityForResult(theIntent, RESULT_EXTERNAL_FILE);
            return true;
        } catch (Exception e) {
            sawim.modules.DebugLog.panic("pickFile", e);
            return false;
        }
    }

    private boolean isCallable(Intent intent) {
        return !getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        sawim.modules.DebugLog.println("result " + requestCode + " " + resultCode + " " + data);
        if (null == data) return;
        if (RESULT_OK != resultCode) return;
        try {
            if (RESULT_PHOTO == requestCode) {
                if (null == photoListener) return;
                photoListener.processPhoto(data.getByteArrayExtra("photo"));
                photoListener = null;

            } else if (RESULT_EXTERNAL_PHOTO == requestCode) {
                if (null == photoListener) return;
                Uri uriImage = data.getData();
                InputStream in = getContentResolver().openInputStream(uriImage);
                byte[] img = new byte[in.available()];
                in.read(img);
                photoListener.processPhoto(img);
                photoListener = null;

            } else if (RESULT_EXTERNAL_FILE == requestCode) {
                Uri fileUri = data.getData();
                sawim.modules.DebugLog.println("File " + fileUri);
                InputStream is = getContentResolver().openInputStream(fileUri);
                fileTransferListener.onFileSelect(is, getFileName(fileUri));
                fileTransferListener = null;
            }
        } catch (Throwable ignored) {
            sawim.modules.DebugLog.panic("activity", ignored);
        }
    }

    public void updateAppIcon() {
        serviceConnection.send(Message.obtain(null, SawimService.UPDATE_APP_ICON));
    }

    private final Object lock = new Object();

    public String getFromClipboard() {
        final AtomicReference<String> text = new AtomicReference<String>();
        text.set(null);
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    text.set(clipboard.hasText() ? clipboard.getText().toString() : null);
                    synchronized (lock) {
                        lock.notify();
                    }
                } catch (Throwable e) {
                    sawim.modules.DebugLog.panic("get clipboard", e);
                    // do nothing
                }
            }
        });
        return text.get();
    }

    public void putToClipboard(final String label, final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setText(text);
                } catch (Throwable e) {
                    sawim.modules.DebugLog.panic("set clipboard", e);
                    // do nothing
                }
            }
        });
    }

    private final SawimServiceConnection serviceConnection = new SawimServiceConnection();

    private String getFileName(Uri fileUri) {
        String file = getRealPathFromUri(fileUri);
        return file.substring(file.lastIndexOf('/') + 1);
    }

    private String getRealPathFromUri(Uri uri) {
        try {
            if ("content".equals(uri.getScheme())) {
                String[] proj = {MediaStore.MediaColumns.DATA};
                Cursor cursor = managedQuery(uri, proj, null, null, null);
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                cursor.moveToFirst();
                return cursor.getString(columnIndex);
            }
            if ("file".equals(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception ignored) {
        }
        return uri.toString();
    }

    public void showHistory(HistoryStorage history) {
        String historyFilePath = history.getAndroidStorage().getTextFile();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("file://" + historyFilePath);
        intent.setDataAndType(uri, "text/plain");
        startActivity(intent);
    }
}