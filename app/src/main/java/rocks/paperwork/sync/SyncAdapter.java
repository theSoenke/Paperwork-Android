package rocks.paperwork.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rocks.paperwork.R;
import rocks.paperwork.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.adapters.NotesAdapter;
import rocks.paperwork.adapters.NotesAdapter.Note;
import rocks.paperwork.adapters.Tag;
import rocks.paperwork.data.DatabaseContract;
import rocks.paperwork.data.DatabaseHelper;
import rocks.paperwork.data.HostPreferences;
import rocks.paperwork.data.NoteDataSource;

public class SyncAdapter extends AbstractThreadedSyncAdapter
{
    private static final String LOG_TAG = SyncAdapter.class.getSimpleName();
    private static SwipeRefreshLayout sSwipeContainer;

    public SyncAdapter(Context context, boolean autoInitialize)
    {
        super(context, autoInitialize);
    }

    public static void syncImmediately(Context context)
    {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    public static void syncImmediately(Context context, SwipeRefreshLayout refreshLayout)
    {
        sSwipeContainer = refreshLayout;

        if (!isNetworkAvailable(context))
        {
            sSwipeContainer.setRefreshing(false);
            Log.d(LOG_TAG, "No internet available");
            return;
        }

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
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name),
                context.getString(R.string.sync_account_type));

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

        if (!HostPreferences.preferencesExist(getContext()))
        {
            Log.d(LOG_TAG, "No user is logged in");
            return;
        }

        try
        {
            uploadNotes(host, hash);
            deleteNotes(host, hash);
            fetch(host, hash, NoteData.notebooks);
            fetch(host, hash, NoteData.notes);
            fetch(host, hash, NoteData.tags);
        }
        catch (JSONException e)
        {
            Log.d(LOG_TAG, "JSONException", e);
        }
        catch (FileNotFoundException e)
        {
            // FIXME workaround because response code is always 200, even if authentication failed
            authenticationFailed();
        }
        catch (IOException e)
        {
            if (sSwipeContainer != null)
            {
                sSwipeContainer.setRefreshing(false);
            }
            Log.d(LOG_TAG, "Error syncing data", e);
        }
    }

    private void fetch(String host, String hash, NoteData data) throws IOException, JSONException
    {
        NoteDataSource dataSource = NoteDataSource.getInstance(getContext());
        SyncManager syncManager = new SyncManager();

        if (data == NoteData.notes)
        {
            String result = fetchTask(host + "/api/v1/notebooks/" + Notebook.DEFAULT_ID + "/notes", hash);
            List<Note> remoteNotes = parseNotes(result);
            List<Note> localNotes = dataSource.getNotes(null);

            Pair<List<Note>, List<Note>> syncedNotes = syncManager.syncNotes(localNotes, remoteNotes);
            dataSource.bulkInsertNotes(syncedNotes.first);
            updateNotes(host, hash, syncedNotes.second);
        }
        else if (data == NoteData.notebooks)
        {
            String result = fetchTask(host + "/api/v1/notebooks/", hash);
            List<Notebook> remoteNotebooks = parseNotebooks(result);
            NoteDataSource.getInstance(getContext()).bulkInsertNotebooks(remoteNotebooks);
        }
        else if (data == NoteData.tags)
        {
            String result = fetchTask(host + "/api/v1/tags/", hash);
            List<Tag> remoteTags = parseTags(result);
            NoteDataSource.getInstance(getContext()).bulkInsertTags(remoteTags);
        }
    }

    /**
     * Uploads all new notes
     */
    private void uploadNotes(String host, String hash) throws IOException, JSONException
    {
        NoteDataSource dataSource = NoteDataSource.getInstance(getContext());
        List<Note> notSyncedNotes = dataSource.getNotes(DatabaseContract.NoteEntry.NOTE_STATUS.not_synced);

        for (Note note : notSyncedNotes)
        {
            Note newNote = modifyNote(host, hash, note, ModifyNote.create_note);
            if (newNote != null)
            {
                dataSource.deleteNote(note);
                dataSource.insertNote(newNote);
            }
            else
            {
                Log.d(LOG_TAG, "Creating note failed");
            }
        }
    }

    /**
     * Delete notes on the server
     */
    private void deleteNotes(String host, String hash) throws IOException, JSONException
    {
        NoteDataSource dataSource = NoteDataSource.getInstance(getContext());
        List<Note> deletedNotes = dataSource.getNotes(DatabaseContract.NoteEntry.NOTE_STATUS.deleted);

        for (Note note : deletedNotes)
        {
            modifyNote(host, hash, note, ModifyNote.delete_note);
            dataSource.deleteNote(note);
        }
    }

    /**
     * Updates notes on the server
     *
     * @param updatedNotes Notes that should be updated on the server
     */
    private void updateNotes(String host, String hash, List<Note> updatedNotes) throws IOException, JSONException
    {
        for (Note localNote : updatedNotes)
        {
            Note result = modifyNote(host, hash, localNote, ModifyNote.update_note);
            if (result != null)
            {
                localNote.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.synced);
                localNote.setUpdatedAt(DatabaseHelper.getCurrentTime());
                NoteDataSource.getInstance(getContext()).insertNote(localNote);
            }
        }
    }

    private Note parseNote(String jsonStr)
    {
        Note note = null;

        try
        {
            JSONObject jsonNote = new JSONObject(jsonStr);
            JSONObject version = jsonNote.getJSONObject("version");

            String id = jsonNote.getString("id");
            String title = version.getString("title");
            String content = version.getString("content");
            Date date = DatabaseHelper.getDateTime(jsonNote.getString("updated_at"));
            String notebookId = jsonNote.getString("notebook_id");

            note = new NotesAdapter.Note(id);
            note.setNotebookId(notebookId);
            note.setTitle(title);
            note.setContent(content);
            note.setUpdatedAt(date);
            note.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.synced);
        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG, "Error parsing JSON " + jsonStr, e);
        }

        return note;
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
                Note note = parseNote(jsonNote.toString());

                notes.add(note);
            }
        }
        catch (JSONException e)
        {
            Log.d(LOG_TAG, "Error parsing JSON: " + jsonStr, e);
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
            Log.d(LOG_TAG, "Error parsing JSON: " + jsonStr, e);
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
            }
        }
        catch (JSONException e)
        {
            Log.d(LOG_TAG, "Error parsing Json" + jsonStr);
        }

        return tags;
    }

    private String fetchTask(String path, String hash) throws IOException
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
                authenticationFailed();
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

        throw new ConnectException();
    }

    private Note modifyNote(String host, String hash, Note note, ModifyNote task) throws IOException, JSONException
    {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonStr;
        String path = "";

        if (task == ModifyNote.create_note)
        {
            path = host + "/api/v1/notebooks/" + note.getNotebookId() + "/notes";
        }
        else if (task == ModifyNote.update_note)
        {
            path = host + "/api/v1/notebooks/" + note.getNotebookId() + "/notes/" + note.getId();
        }
        else if (task == ModifyNote.delete_note)
        {
            path = host + "/api/v1/notebooks/" + note.getNotebookId() + "/notes/" + note.getId();
        }

        try
        {
            URL url = new URL(path);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Authorization", "Basic " + hash);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(15000);

            // create json note
            JSONObject jsonNote = new JSONObject();
            jsonNote.put("title", note.getTitle());
            jsonNote.put("content", note.getContent());

            if (task == ModifyNote.create_note)
            {
                urlConnection.setRequestMethod("POST");
                jsonNote.put("content_preview", note.getPreview());
            }
            else if (task == ModifyNote.update_note)
            {
                urlConnection.setRequestMethod("PUT");
            }
            else if (task == ModifyNote.delete_note)
            {
                urlConnection.setRequestMethod("DELETE");
            }
            else
            {
                Log.e(LOG_TAG, "Task does not exist");
            }

            urlConnection.connect();

            if (task != ModifyNote.delete_note)
            {
                OutputStream outputStream = urlConnection.getOutputStream();
                outputStream.write(jsonNote.toString().getBytes());
                outputStream.flush();
                outputStream.close();
            }

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
                authenticationFailed();
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

                if (task == ModifyNote.create_note)
                {
                    JSONObject jsonResponse = json.getJSONObject("response");
                    return parseNote(jsonResponse.toString());
                }

                return new Note("");
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

        Log.d(LOG_TAG, "Failed to modify note");
        throw new ConnectException();
    }

    private void authenticationFailed()
    {
        // TODO handle authentication failure
        Log.d(LOG_TAG, "Authentication failed");
    }

    private enum ModifyNote
    {
        create_note,
        update_note,
        delete_note
    }

    private enum NoteData
    {
        notes,
        notebooks,
        tags
    }

    private static boolean isNetworkAvailable(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    public class SyncManager
    {
        /**
         * Syncs local and remote notes
         *
         * @param localNotes  Locally stored notes
         * @param remoteNotes Notes from the server
         * @return First list contains notes that need to be updated locally, second list of notes need to be updated on the server
         */
        public Pair<List<Note>, List<Note>> syncNotes(List<Note> localNotes, List<Note> remoteNotes)
        {
            Map<String, Note> noteIds = new HashMap<>();
            List<Note> updatedNotes = new ArrayList<>();
            List<Note> updateOnServer = new ArrayList<>();

            NoteDataSource dataSource = NoteDataSource.getInstance(getContext());

            for (Note remoteNote : remoteNotes)
            {
                noteIds.put(remoteNote.getId(), remoteNote);
                if (!localNotes.contains(remoteNote))
                {
                    updatedNotes.add(remoteNote);
                }
            }

            for (Note localNote : localNotes)
            {
                if (!noteIds.containsKey(localNote.getId()))
                {
                    if (localNote.getSyncStatus() == DatabaseContract.NoteEntry.NOTE_STATUS.synced)
                    {
                        dataSource.deleteNote(localNote);
                    }
                }
                else
                {
                    Note remoteNote = noteIds.get(localNote.getId());

                    if (remoteNote == null)
                    {
                        Log.e(LOG_TAG, "Error accessing note");
                        continue;
                    }

                    if (localNote.getSyncStatus() == DatabaseContract.NoteEntry.NOTE_STATUS.edited)
                    {
                        if (localNote.getUpdatedAt().after(remoteNote.getUpdatedAt()))
                        {
                            // local note is newer
                            updateOnServer.add(localNote);
                        }
                    }
                    else if (localNote.getUpdatedAt().before(remoteNote.getUpdatedAt()))
                    {
                        // remote note is newer
                        if (localNote.getSyncStatus() == DatabaseContract.NoteEntry.NOTE_STATUS.synced)
                        {
                            updatedNotes.add(remoteNote);
                        }
                        else if (localNote.getSyncStatus() == DatabaseContract.NoteEntry.NOTE_STATUS.edited)
                        {
                            // FIXME note was edited locally and on the server -> conflict
                            Log.d(LOG_TAG, "Sync conflict");
                        }
                    }
                }
            }

            return new Pair<>(updatedNotes, updateOnServer);
        }
    }
}
