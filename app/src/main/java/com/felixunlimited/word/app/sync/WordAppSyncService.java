package com.felixunlimited.word.app.sync;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class WordAppSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static WordAppSyncAdapter sWordAppSyncAdapter = null;
    Context mContext = getBaseContext();

    @Override
    public void onCreate() {
//        log("WordAppSyncService", "onCreate - WordAppSyncService", Utility.getUniquePsuedoID(), System.currentTimeMillis(), 'd', null, mContext);
        synchronized (sSyncAdapterLock) {
            if (sWordAppSyncAdapter == null) {
                sWordAppSyncAdapter = new WordAppSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sWordAppSyncAdapter.getSyncAdapterBinder();
    }
}