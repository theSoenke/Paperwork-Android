package rocks.paperwork.android.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SyncService extends Service
{
    private static final String LOG_TAG = SyncService.class.getName();
    private static final Object sSyncAdapterLock = new Object();
    private static SyncAdapter sSyncAdapter;

    @Override
    public void onCreate()
    {
        Log.d(LOG_TAG, "onCreate - SyncService");
        synchronized (sSyncAdapterLock)
        {
            if (sSyncAdapter == null)
            {
                sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
