package rocks.paperwork.android.sync.api;

import android.accounts.AuthenticatorException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fetches JSON from the server
 */
public class FetchTask
{
    private static final String LOG_TAG = FetchTask.class.getSimpleName();

    public static String fetchData(String path, String hash) throws IOException, AuthenticatorException
    {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonStr;

        try
        {
            URL url = new URL(path);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + hash);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(15000);
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

            int responseCode = urlConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)
            {
                throw new AuthenticatorException("Authentication failed");
            }
            else if (responseCode != HttpURLConnection.HTTP_OK)
            {
                Log.e(LOG_TAG, "Response code: " + urlConnection.getResponseCode());
                throw new ConnectException();
            }
            else
            {
                return jsonStr;
            }
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
}
