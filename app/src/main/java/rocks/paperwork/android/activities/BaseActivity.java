package rocks.paperwork.android.activities;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;

import com.phraseapp.android.sdk.PhraseApp;
import com.phraseapp.android.sdk.ScreenshotUI;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(PhraseApp.wrap(newBase));
    }

    @Override
    protected void onResume(){
        super.onResume();
        ScreenshotUI.render(this);
    }
}
