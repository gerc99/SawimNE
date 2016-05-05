package protocol.net;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimException;
import ru.sawim.comm.JLocale;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.RosterHelper;

public abstract class ClientConnection implements Runnable {
    private long keepAliveInterv;
    private boolean usePong;
    protected boolean connect;

    private long nextPingTime;
    private long pongTime;
    private long lastConnect = 0;

    public static final int PING_INTERVAL = 300;
    private static final int PONG_TIMEOUT = 4 * 60;
    private BroadcastReceiver receiver;
    private AlarmManager alarm;
    private PendingIntent pendingIntent;

    protected final void setPingInterval(long interval) {
        keepAliveInterv = Math.min(keepAliveInterv, interval);
        nextPingTime = getCurrentGmtTime() + keepAliveInterv;
    }

    protected final long getPingInterval() {
        return keepAliveInterv;
    }

    protected final void usePong() {
        usePong = true;
        updateTimeout();
    }

    private void initPingValues() {
        usePong = false;
        keepAliveInterv = PING_INTERVAL;
        nextPingTime = getCurrentGmtTime() + keepAliveInterv;
        wakeup(nextPingTime);
    }

    public final void start() {
        new Thread(this, "ClientConnection").start();
    }

    public final void run() {
        initPingValues();
        registerAlarm();
        SawimException exception = null;
        try {
            getProtocol().setConnectingProgress(1);
            connect();
            pingForPong();
            lastConnect = System.currentTimeMillis();
            while (isConnected()) {
                boolean doing = processPacket();
                if (!doing) {
                    sleep(250);
                    doPingIfNeeeded();
                }
            }
        } catch (SawimException e) {
            exception = e;
        } catch (OutOfMemoryError err) {
            exception = new SawimException(100, 2);
        } catch (Exception ex) {
            if (null != getProtocol()) {
                DebugLog.panic("die " + getId(), ex);
            }
            exception = new SawimException(100, 1);
        }
        if (null != exception) {
            try {
                Protocol p = getProtocol();
                if (null != p) {
                    p.processException(exception);
                }
            } catch (Exception ex) {
                DebugLog.panic("die2 " + getId(), ex);
            }
        }
        lastConnect = 0;
        disconnect();
        unregisterAlarm();
        try {
            closeSocket();
        } catch (Exception e) {
            DebugLog.panic("die3 " + getId(), e);
        }
        connect = false;
    }

    public long getLastSessionEstablished() {
        return lastConnect;
    }

    private String getId() {
        Protocol p = getProtocol();
        if (null != p) {
            return p.getUserId();
        }
        return "" + this;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    private void doPingIfNeeeded() throws SawimException {
        long now = getCurrentGmtTime();
        if (usePong && (pongTime + PONG_TIMEOUT < now)) {
            throw new SawimException(120, 9);
        }
        if (nextPingTime <= now) {
            if (!Options.getBoolean(JLocale.getString(R.string.pref_wake_lock))) {
                if (usePong) {
                    pingForPong();
                } else {
                    ping();
                }
            } else wakeup(nextPingTime);
            nextPingTime = now + keepAliveInterv;
        }
    }

    protected final void updateTimeout() {
        pongTime = getCurrentGmtTime();
    }

    public final boolean isConnected() {
        return connect;
    }

    private long getCurrentGmtTime() {
        return SawimApplication.getCurrentGmtTime() / 1000;
    }

    protected void pingForPong() throws SawimException {
    }

    public abstract void disconnect();

    protected abstract Protocol getProtocol();

    protected abstract void closeSocket();

    protected abstract void connect() throws SawimException;

    protected abstract void ping() throws SawimException;

    protected abstract boolean processPacket() throws SawimException;

    private void registerAlarm() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            if (usePong) {
                                pingForPong();
                            } else {
                                ping();
                            }
                        } catch (SawimException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        };

        Context context = SawimApplication.getContext();
        context.registerReceiver(receiver, new IntentFilter("ru.sawim.alarm"));
        pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent("ru.sawim.alarm"), PendingIntent.FLAG_CANCEL_CURRENT);
        alarm = (AlarmManager)(context.getSystemService(Context.ALARM_SERVICE));
    }

    private void unregisterAlarm() {
        alarm.cancel(pendingIntent);
        SawimApplication.getContext().unregisterReceiver(receiver);
    }

    private void wakeup(long nextPingTime) {
        if (Options.getBoolean(JLocale.getString(R.string.pref_wake_lock))) {
            RosterHelper cl = RosterHelper.getInstance();
            if (cl.isConnected()) {
                if (alarm != null) {
                    try {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                            alarm.cancel(pendingIntent);
                            alarm.set(AlarmManager.RTC_WAKEUP, nextPingTime, pendingIntent);
                        } else {
                            alarm.cancel(pendingIntent);
                            setAlarmKitkat(AlarmManager.RTC_WAKEUP, nextPingTime, pendingIntent);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @TargetApi(19)
    private void setAlarmKitkat(int rtcWakeup, long time, PendingIntent pendingIntent) {
        alarm.setExact(rtcWakeup, time, pendingIntent);
    }

}