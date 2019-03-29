package rocks.paperwork.android;

import android.app.Application;

import com.phraseapp.android.sdk.PhraseApp;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PhraseApp.setup(this, "", "");
        PhraseApp.enableScreenshots("", "");
        PhraseApp.updateTranslations();
    }
}