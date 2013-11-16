package ru.sawim;

import android.text.TextUtils;

import java.lang.ref.WeakReference;
import java.util.Vector;


public class LeaksManager {

    private static LeaksManager mThis = null;
    private final Vector<WeakReference<Object>> mRefs = new Vector<WeakReference<Object>>();

    private LeaksManager() {
        super();
    }

    public static LeaksManager getThis() {
        if (mThis == null) {
            mThis = new LeaksManager();
        }
        return mThis;
    }

    public <T> T monitorObject(T obj) {
        if (obj == null) {
            return obj;
        }

        for (WeakReference<Object> ref : mRefs) {
            if (ref.get() == obj) {
                return obj;
            }
        }

        mRefs.add(new WeakReference<Object>(obj));

        return obj;
    }

    public Vector<String> checkLeaks() {
        System.gc();

        Vector<String> names = new Vector<String>();

        for (int i = mRefs.size() - 1; i >= 0; i--) {
            WeakReference<Object> ref = mRefs.elementAt(i);
            Object obj = ref.get();
            if (obj != null) {
                String className = obj.getClass().getSimpleName();
                addUniqueClassName(names, TextUtils.isEmpty(className) ? "Unknown class name" : className);
            } else {
                mRefs.remove(i);
            }
        }

        mRefs.trimToSize();

        return names;
    }

    private void addUniqueClassName(Vector<String> names, String className) {
        int index = -1;
        for (int j = 0; j < names.size(); j++) {
            if (names.elementAt(j).equals(className)) {
                index = j;
                break;
            }
        }

        if (index == -1) {
            names.add(className);
        }
    }
}

