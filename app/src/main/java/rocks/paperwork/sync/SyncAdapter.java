package rocks.paperwork.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rocks.paperwork.R;
import rocks.paperwork.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.adapters.NotesAdapter;
import rocks.paperwork.adapters.NotesAdapter.Note;
import rocks.paperwork.adapters.Tag;
import rocks.paperwork.data.DatabaseHelper;
import rocks.paperwork.data.HostPreferences;
import rocks.paperwork.data.NoteDataSource;

public class SyncAdapter extends AbstractThreadedSyncAdapter
{
    private final String LOG_TAG = SyncAdapter.class.getSimpleName();

    public SyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);
    }

    /**
     * sync notes immediately
     *
     * @param context
     */
    public static void syncImmediately(Context context)
    {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    // TODO use paperwork account instead of fake account
    public static Account getSyncAccount(Context context)
    {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount))
        {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null))
            {
                return null;
            }
        }
        return newAccount;
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult)
    {
        Log.d(LOG_TAG, "onPerformSync Called.");

        String host = HostPreferences.readSharedSetting(getContext(), HostPreferences.HOST, "");
        String hash = HostPreferences.readSharedSetting(getContext(), HostPreferences.HASH, "");

        // TODO compare remote and local data and syncchronize both

        String url = host + "/api/v1/notebooks/" + Notebook.DEFAULT_ID + "/notes";
        String result = fetchTask(url, hash);
        List<Note> remoteNotes = parseNotes(result);
        NoteDataSource.getInstance(getContext()).bulkInsertNotes(remoteNotes);

        url = host + "/api/v1/notebooks/";
        result = fetchTask(url, hash);
        List<Notebook> remoteNotebooks = parseNotebooks(result);

        url = host + "/api/v1/tags/";
        result = fetchTask(url, hash);
        List<Tag> remoteTags = parseTags(result);
    }

    private List<Note> parseNotes(String jsonStr)
    {
        List<Note> notes = new ArrayList<>();

        JSONObject jsonData;
        try
        {
            jsonData = new JSONObject(jsonStr);

            JSONArray jsonNotes = jsonData.getJSONArray("response");

            for (int i = 0; i < jsonNotes.length(); i++)
            {
                JSONObject jsonNote = jsonNotes.getJSONObject(i);
                JSONObject version = jsonNote.getJSONObject("version");

                String id = jsonNote.getString("id");
                String title = version.getString("title");
                String content = version.getString("content");
                Date date = DatabaseHelper.getDateTime(jsonNote.getString("updated_at"));
                String notebookId = jsonNote.getString("notebook_id");

                Note note = new NotesAdapter.Note(id);
                note.setNotebookId(notebookId);
                note.setTitle(title);
                note.setContent(content);
                note.setUpdatedAt(date);

                notes.add(note);
            }
        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG, "Error parsing JSON");
        }

        return notes;
    }

    private List<Notebook> parseNotebooks(String jsonStr)
    {
        List<Notebook> notebooks = new ArrayList<>();

        try
        {
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
        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG, "Error parsing JSON" + jsonStr, e);
        }

        return notebooks;
    }

    private List<Tag> parseTags(String jsonStr)
    {
        List<Tag> tags = new ArrayList<>();

        try
        {
            JSONObject jsonData = new JSONObject(jsonStr);
            JSONArray jsonTags = jsonData.getJSONArray("response");

            for (int i = 0; i < jsonTags.length(); i++)
            {
                JSONObject tagJson = jsonTags.getJSONObject(i);
                String id = tagJson.getString("id");
                String title = tagJson.getString("title");

                Tag tag = new Tag(id);
                tag.setTitle(title);

                tags.add(tag);
                NoteDataSource.getInstance(getContext()).insertTag(tag);
            }
        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG, "Error parsing Json" + jsonStr);
        }

        return tags;
    }


    private String fetchTask(String path, String hash)
    {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonStr = "";

        try
        {
            URL url = new URL(path);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + hash);
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(10000);
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
                authenticationFailed();
            }
            else if (responseCode != HttpURLConnection.HTTP_OK)
            {
                Log.e(LOG_TAG, "Response code: " + urlConnection.getResponseCode());
            }
        }
        catch (SocketTimeoutException e)
        {
            Log.d(LOG_TAG, "Timeout");
        }
        catch (FileNotFoundException e)
        {
            // FIXME workaround because response code is always 200, even if authentication failed
            authenticationFailed();
        }
        catch (IOException e)
        {
            Log.e(LOG_TAG, "IO Exception", e);
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

        if (jsonStr.isEmpty())
        {
            Log.e(LOG_TAG, "Result is empty");
        }

        return jsonStr;
    }

    private void authenticationFailed()
    {
        // TODO handle authentication failure
    }
}
