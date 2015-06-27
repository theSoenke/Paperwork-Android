package rocks.paperwork.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;

import rocks.paperwork.R;
import rocks.paperwork.data.HostPreferences;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity
{
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    // UI references.
    private TextInputLayout mHostView;
    private TextInputLayout mEmailView;
    private TextInputLayout mPasswordView;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mHostView = (TextInputLayout) findViewById(R.id.host);
        mEmailView = (TextInputLayout) findViewById(R.id.email);
        mPasswordView = (TextInputLayout) findViewById(R.id.password);

        mPasswordView.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent)
            {
                if (id == R.id.login || id == EditorInfo.IME_NULL)
                {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                attemptLogin();
            }
        });

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.wait));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.primaryDarkColor));
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin()
    {
        if (mAuthTask != null)
        {
            return;
        }

        // Reset errors.
        mHostView.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String host = mHostView.getEditText().getText().toString();
        host = formatUrl(host);
        String email = mEmailView.getEditText().getText().toString();
        String password = mPasswordView.getEditText().getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid url
        if (TextUtils.isEmpty(host))
        {
            mHostView.setError(getString(R.string.error_field_required));
            cancel = true;
        }
        else if (!isUrlValid(host))
        {
            mHostView.setError(getString(R.string.error_invalid_url));
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password))
        {
            mPasswordView.setError(getString(R.string.error_incorrect_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email))
        {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }
        else if (!isEmailValid(email))
        {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel)
        {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            if (focusView != null)
            {
                focusView.requestFocus();
            }
        }
        else
        {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            final String hash = Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP);

            HostPreferences.saveSharedSetting(this, "host", host);
            HostPreferences.saveSharedSetting(this, "hash", hash);
            HostPreferences.saveSharedSetting(this, "email", email);

            mAuthTask = new UserLoginTask(hash);
            mAuthTask.execute(host + "/api/v1/notebooks");
        }
    }

    private boolean isUrlValid(String url)
    {
        return Patterns.WEB_URL.matcher(url).matches();
    }

    private boolean isEmailValid(String email)
    {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private String formatUrl(String host)
    {
        if (host.length() >= 4 && !host.substring(0, 4).toLowerCase().equals("http"))
        {
            return "http://" + host;
        }
        return host;
    }

    /**
     * Shows the progress UI
     */
    private void showProgress(final boolean show)
    {
        if (show)
        {
            mProgressDialog.show();
        }
        else
        {
            mProgressDialog.dismiss();
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<String, Void, Integer>
    {
        private final String LOG_TAG = UserLoginTask.class.getName();
        private final String mHash;

        private final int SUCCESS = 0;
        private final int CONNECTION_FAILED = 1;
        private final int FILE_NOT_FOUND = 2;
        private final int IO_EXCEPTION = 3;

        public UserLoginTask(String hash)
        {
            mHash = hash;
        }

        @Override
        protected Integer doInBackground(String... params)
        {
            HttpURLConnection urlConnection = null;

            try
            {
                URL url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", "Basic " + mHash);
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(10000);
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                urlConnection.getInputStream();
            }
            catch (SocketTimeoutException e)
            {
                Log.d(LOG_TAG, "Timeout");
                return CONNECTION_FAILED;
            }
            catch (ConnectException e)
            {
                Log.d(LOG_TAG, "Failed to connect");
                return CONNECTION_FAILED;
            }
            catch (FileNotFoundException e)
            {
                return FILE_NOT_FOUND;
            }
            catch (SSLHandshakeException e)
            {
                Log.d(LOG_TAG, "SSL Certificate is not valid");
                return CONNECTION_FAILED;
            }
            catch (IOException e)
            {
                Log.e(LOG_TAG, "IO Exception", e);
                return IO_EXCEPTION;
            }
            finally
            {
                if (urlConnection != null)
                {
                    urlConnection.disconnect();
                }
            }

            return SUCCESS;
        }

        @Override
        protected void onPostExecute(final Integer result)
        {
            if (result == SUCCESS)
            {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            else if (result == FILE_NOT_FOUND)
            {
                HostPreferences.clearPreferences(LoginActivity.this);
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
            else
            {
                HostPreferences.clearPreferences(LoginActivity.this);
                showTimeoutDialog();
            }

            mAuthTask = null;
            showProgress(false);
        }

        @Override
        protected void onCancelled()
        {
            mAuthTask = null;
            showProgress(false);
        }
    }

    private void showTimeoutDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.connection_error).setMessage(R.string.connection_error_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

