package rocks.paperwork.android.sync.api;

import android.accounts.AuthenticatorException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rocks.paperwork.android.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.android.data.DatabaseHelper;

/**
 * Syncs notebooks with the server
 */
public class NotebookSync
{
    private static final String LOG_TAG = NotebookSync.class.getSimpleName();


    private static Notebook createNotebook(String host, String hash, Notebook notebook) throws IOException, JSONException, AuthenticatorException
    {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonStr;
        String path = host + "/api/v1/notebooks/";

        try
        {
            URL url = new URL(path);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Authorization", "Basic " + hash);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(15000);
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();

            // create json note
            JSONObject jsonNotebook = new JSONObject();
            jsonNotebook.put("type", 0);
            jsonNotebook.put("title", notebook.getTitle());
            jsonNotebook.put("shortcut", "");

            OutputStream outputStream = urlConnection.getOutputStream();
            outputStream.write(jsonNotebook.toString().getBytes());
            outputStream.flush();
            outputStream.close();

            // read response
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
                Log.d(LOG_TAG, "Error while creating note, response code: " + urlConnection.getResponseCode());
                throw new ConnectException();
            }
            else
            {
                JSONObject json = new JSONObject(jsonStr);

                if (!json.getBoolean("success"))
                {
                    throw new ConnectException();
                }

                JSONObject jsonResponse = json.getJSONObject("response");
                return parseNotebook(jsonResponse.toString());
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

    private static Notebook parseNotebook(String jsonStr) throws JSONException
    {
        JSONObject jsonNotebook = new JSONObject(jsonStr);

        String id = jsonNotebook.getString("id");
        String title = jsonNotebook.getString("title");
        Date date = DatabaseHelper.getDateTime(jsonNotebook.getString("updated_at"));

        Notebook notebook = new Notebook(id);
        notebook.setTitle(title);
        notebook.setUpdatedAt(date);

        return notebook;
    }

    public static List<Notebook> parseNotebooks(String jsonStr) throws JSONException
    {
        List<Notebook> notebooks = new ArrayList<>();

        JSONObject jsonData = new JSONObject(jsonStr);
        JSONArray jsonNotebooks = jsonData.getJSONArray("response");

        for (int i = 0; i < jsonNotebooks.length(); i++)
        {
            JSONObject notebookJson = jsonNotebooks.getJSONObject(i);
            String id = notebookJson.getString("id");
            String title = notebookJson.getString("title");

            if (!id.equals(Notebook.DEFAULT_ID))
            {
                Notebook notebook = new Notebook(id);
                notebook.setTitle(title);

                notebooks.add(notebook);
            }
        }

        return notebooks;
    }
}
