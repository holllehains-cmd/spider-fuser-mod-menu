package com.suitmod;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class SuitModProvider extends ContentProvider {
    static final String TAG = "SuitMod";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static volatile boolean sCallbacksRegistered = false;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "SuitModProvider.onCreate");
        tryRegisterCallbacks(getContext());
        return true;
    }

    static synchronized void tryRegisterCallbacks(Context ctx) {
        if (sCallbacksRegistered || ctx == null) {
            return;
        }
        Context appCtx = ctx.getApplicationContext();
        Application app = null;
        if (appCtx instanceof Application) {
            app = (Application) appCtx;
        } else if (ctx instanceof Application) {
            app = (Application) ctx;
        }

        if (app != null) {
            SuitModMenu.Installer installer = new SuitModMenu.Installer();
            app.registerActivityLifecycleCallbacks(installer);
            sCallbacksRegistered = true;
            Log.d(TAG, "ActivityLifecycleCallbacks registered on " + app.getClass().getName());
        } else {
            Log.e(TAG, "Context is not Application, scheduling retry");
            MAIN_HANDLER.postDelayed(() -> {
                if (!sCallbacksRegistered) {
                    tryRegisterCallbacks(ctx);
                }
            }, 500L);
        }
    }

    static void notifyActivityReady(Activity activity) {
        if (activity == null) {
            return;
        }
        tryRegisterCallbacks(activity);
    }

    @Override
    public Cursor query(Uri u, String[] p, String s, String[] sa, String so) {
        return null;
    }

    @Override
    public String getType(Uri u) {
        return null;
    }

    @Override
    public Uri insert(Uri u, ContentValues v) {
        return null;
    }

    @Override
    public int delete(Uri u, String s, String[] sa) {
        return 0;
    }

    @Override
    public int update(Uri u, ContentValues v, String s, String[] sa) {
        return 0;
    }
}
