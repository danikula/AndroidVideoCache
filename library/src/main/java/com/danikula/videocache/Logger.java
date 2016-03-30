package com.danikula.videocache;

import android.util.Log;

/**
 * Created by Dev1 on 30.03.2016.
 */
public final class Logger {

    private String mTag;

    private boolean mEnabled = false;

    public Logger(String tag) {
        mTag = tag;
    }

    public void d(String msg, Throwable e) {
        log(Log.DEBUG, 'D', msg, e);
    }

    public void v(String msg, Throwable e) {
        log(Log.VERBOSE, 'V', msg, e);
    }

    public void i(String msg, Throwable e) {
        log(Log.INFO, 'I', msg, e);
    }

    public void w(String msg, Throwable e) {
        log(Log.WARN, 'W', msg, e);
    }

    public void e(String msg, Throwable e) {
        log(Log.ERROR, 'E', msg, e);
    }

    public void d(String msg) {
        log(Log.DEBUG, 'D', msg);
    }

    public void v(String msg) {
        log(Log.VERBOSE, 'V', msg);
    }

    public void i(String msg) {
        log(Log.INFO, 'I', msg);
    }

    public void w(String msg) {
        log(Log.WARN, 'W', msg);
    }

    public void e(String msg) {
        log(Log.ERROR, 'E', msg);
    }

    private void log(int level, char levelShort, String msg, Throwable e) {
        if (mEnabled || level > Log.INFO) {
            Log.println(level, mTag, msg);
        }
    }

    private void log(int level, char levelShort, String msg) {
        if (mEnabled || level > Log.INFO) {
            Log.println(level, mTag, msg);
        }
    }
}
