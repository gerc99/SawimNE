package ru.sawim;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 12.06.13
 * Time: 17:42
 * To change this template use File | Settings | File Templates.
 */

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final DateFormat formatter = new SimpleDateFormat("dd.MM.yy HH:mm");
    private static final DateFormat fileFormatter = new SimpleDateFormat("dd-MM-yy");
    private String versionName = "0";
    private int versionCode = 0;
    private final String stacktraceDir;
    private final Thread.UncaughtExceptionHandler previousHandler;

    private static final String PATH = "/Android/data/%s/files/";

    private ExceptionHandler(Context context, boolean chained) {
        PackageManager mPackManager = context.getPackageManager();
        PackageInfo mPackInfo;
        try {
            mPackInfo = mPackManager.getPackageInfo(context.getPackageName(), 0);
            versionName = mPackInfo.versionName;
            versionCode = mPackInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
        if (chained)
            previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        else
            previousHandler = null;
        stacktraceDir = String.format(PATH, context.getPackageName());
    }

    public static ExceptionHandler inContext(Context context) {
        return new ExceptionHandler(context, true);
    }

    public static ExceptionHandler reportOnlyHandler(Context context) {
        return new ExceptionHandler(context, false);
    }

    private void writeException(String exception) {
        final Date dumpDate = new Date(System.currentTimeMillis());
        File sd = Environment.getExternalStorageDirectory();
        File stacktrace = new File(
                sd.getPath() + stacktraceDir,
                String.format(
                        previousHandler != null ? "stacktrace-%s.txt" : "logs-%s.txt",
                        fileFormatter.format(dumpDate)));
        File dumpdir = stacktrace.getParentFile();
        boolean dirReady = dumpdir.isDirectory() || dumpdir.mkdirs();
        if (dirReady) {
            FileWriter writer = null;
            try {
                writer = new FileWriter(stacktrace, true);
                writer.write(exception);
            } catch (IOException e) {
            } finally {
                try {
                    if (writer != null)
                        writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void writeLog(final String tag, final String log) {
        Log.d(tag, log);
        SawimApplication.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final Date dumpDate = new Date(System.currentTimeMillis());
                File sd = Environment.getExternalStorageDirectory();
                File stacktrace = new File(sd.getPath() + String.format(PATH, SawimApplication.getContext().getPackageName()),
                        String.format("%s-logs-%s.txt",
                                tag, fileFormatter.format(dumpDate)));
                File dumpdir = stacktrace.getParentFile();
                boolean dirReady = dumpdir.isDirectory() || dumpdir.mkdirs();
                if (dirReady) {
                    FileWriter writer = null;
                    try {
                        writer = new FileWriter(stacktrace, true);
                        writer.write(log + "\n");
                    } catch (IOException e) {
                    } finally {
                        try {
                            if (writer != null)
                                writer.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        });
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        SawimApplication.getInstance().quit(true);
        final String state = Environment.getExternalStorageState();
        final Date dumpDate = new Date(System.currentTimeMillis());
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            StringBuilder reportBuilder = new StringBuilder();
            reportBuilder
                    .append("\n\n\n")
                    .append(formatter.format(dumpDate)).append("\n")
                    .append(String.format("Version: %s (%d)\n", versionName, versionCode));
            if (thread != null)
                reportBuilder.append(thread.toString()).append("\n");
            processThrowable(exception, reportBuilder);
            writeException(reportBuilder.toString());
        }
        if (previousHandler != null)
            previousHandler.uncaughtException(thread, exception);
    }

    private void processThrowable(Throwable exception, StringBuilder builder) {
        if (exception == null)
            return;
        StackTraceElement[] stackTraceElements = exception.getStackTrace();
        builder
                .append("Exception: ").append(exception.getClass().getName()).append("\n")
                .append("Message: ").append(exception.getMessage()).append("\nStacktrace:\n");
        for (StackTraceElement element : stackTraceElements) {
            builder.append("\t\tat ").append(element.toString()).append("\n");
        }
        processThrowable(exception.getCause(), builder);
    }
}