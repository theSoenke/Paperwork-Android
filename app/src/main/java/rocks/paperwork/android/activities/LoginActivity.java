package rocks.paperwork.android.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;

import rocks.paperwork.android.R;
import rocks.paperwork.android.data.HostPreferences;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity
{
    private UserLoginTask mAuthTask = null; // Keep track of the login task to ensure we can cancel it if requested.
    private TextInputLayout mHostView;
    private TextInputLayout mUserView;
    private TextInputLayout mPasswordView;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mHostView = (TextInputLayout) findViewById(R.id.host_url);
        mUserView = (TextInputLayout) findViewById(R.id.user);
        mPasswordView = (TextInputLayout) findViewById(R.id.password);

        mPasswordView.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent)
            {
                if (id == R.id.login || id == EditorInfo.IME_ACTION_DONE)
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
        mUserView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String host = mHostView.getEditText().getText().toString();
        host = formatUrl(host);
        String user = mUserView.getEditText().getText().toString();
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

        // Check for a valid user address.
        if (TextUtils.isEmpty(user))
        {
            mUserView.setError(getString(R.string.error_field_required));
            focusView = mUserView;
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

            final String hash = Base64.encodeToString((user + ":" + password).getBytes(), Base64.NO_WRAP);

            HostPreferences.saveSharedSetting(this, "host", host);
            HostPreferences.saveSharedSetting(this, "hash", hash);
            HostPreferences.saveSharedSetting(this, "user", user);

            mAuthTask = new UserLoginTask(hash);
            mAuthTask.execute(host + "/api/v1/notebooks");
        }
    }

    private boolean isUrlValid(String url)
    {
        return Patterns.WEB_URL.matcher(url).matches();
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
            BufferedReader reader = null;
            String jsonStr;

            try
            {
                URL url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", "Basic " + mHash);
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(20000);
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder builder = new StringBuilder();
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null)
                {
                    builder.append(line).append("\n");
                }
                jsonStr = builder.toString();

                JSONObject result = new JSONObject(jsonStr);
                if (result.getBoolean("success"))
                {
                    return SUCCESS;
                }
                else
                {
                    return FILE_NOT_FOUND;
                }
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
                Log.d(LOG_TAG, "FileNotFoundException");
                return FILE_NOT_FOUND;
            }
            catch (SSLHandshakeException e)
            {
                Log.d(LOG_TAG, "SSL Certificate is not valid");
                return CONNECTION_FAILED;
            }
            catch (JSONException e)
            {
                Log.d(LOG_TAG, "Error parsing JSON");
                return FILE_NOT_FOUND;
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

                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (final IOException e)
                    {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
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
                mHostView.setError(getString(R.string.error_timeout));
                mHostView.requestFocus();
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
}

