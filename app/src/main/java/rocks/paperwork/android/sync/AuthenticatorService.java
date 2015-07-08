package rocks.paperwork.android.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service allows the sync adapter frame to access the authenticator
 */
public class AuthenticatorService extends Service
{
    private Authenticator mAuthenticator;

    @Override
    public IBinder onBind(Intent intent)
    {
        return mAuthenticator.getIBinder();
    }

    @Override
    public void onCreate()
    {
        mAuthenticator = new Authenticator(this);
    }
}
