package rocks.paperwork.android.activities;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import com.phraseapp.android.sdk.PhraseApp;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(PhraseApp.wrap(newBase));
    }
}
