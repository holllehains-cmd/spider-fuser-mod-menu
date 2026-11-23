package com.suitmod;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.app.Application;
import android.util.Log;

public class SuitModProvider extends ContentProvider {
    static final String TAG = "SuitMod";

    @Override
    public boolean onCreate() {
        Log.d(TAG, "SuitModProvider.onCreate");
        try {
            Context ctx = getContext();
            if (ctx != null) {
                Context appCtx = ctx.getApplicationContext();
                if (appCtx instanceof Application) {
                    ((Application) appCtx).registerActivityLifecycleCallbacks(new SuitModMenu.Installer());
                } else if (ctx instanceof Application) {
                    ((Application) ctx).registerActivityLifecycleCallbacks(new SuitModMenu.Installer());
                } else {
                    Log.e(TAG, "Context is not Application, cannot register callbacks");
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to install menu", t);
        }
        return true;
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] sa, String so) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] sa) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] sa) { return 0; }
}
