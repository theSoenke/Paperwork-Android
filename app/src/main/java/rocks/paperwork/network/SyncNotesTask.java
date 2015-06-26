package rocks.paperwork.network;

import android.content.Context;
import android.os.AsyncTask;
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
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import rocks.paperwork.activities.MainActivity;
import rocks.paperwork.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.adapters.NotesAdapter;
import rocks.paperwork.adapters.NotesAdapter.Note;
import rocks.paperwork.adapters.Tag;
import rocks.paperwork.data.DatabaseHelper;
import rocks.paperwork.data.HostPreferences;
import rocks.paperwork.data.NotesDataSource;
import rocks.paperwork.fragments.NotebooksFragment;
import rocks.paperwork.fragments.NotesFragment;

/**
 * Syncs notes between server and local
 */

public class SyncNotesTask
{
    private final String LOG_TAG = SyncNotesTask.class.getName();
    private final Context mContext;


    public SyncNotesTask(Context context)
    {
        mContext = context;
    }

    private enum FetchTaskName
    {
        notebooks,
        notes,
        tags
    }

    private enum ModifyNote
    {
        create_note,
        update_note,
        delete_note
    }

    public void fetchAllData()
    {
        fetchNotebooks();
        fetchNotes();
        fetchTags();
    }

    /**
     * fetches all notebooks from the server
     */
    public void fetchNotebooks()
    {
        String host = HostPreferences.readSharedSetting(mContext, HostPreferences.HOST, "");
        String hash = HostPreferences.readSharedSetting(mContext, HostPreferences.HASH, "");
        new FetchTask(FetchTaskName.notebooks, hash).execute(host + "/api/v1/notebooks/");
    }

    /**
     * fetches all notes from the server
     */
    public void fetchNotes()
    {
        String host = HostPreferences.readSharedSetting(mContext, HostPreferences.HOST, "");
        String hash = HostPreferences.readSharedSetting(mContext, HostPreferences.HASH, "");
        new FetchTask(FetchTaskName.notes, hash).execute(host + "/api/v1/notebooks/" + Notebook.DEFAULT_ID + "/notes");
    }

    /**
     * fetches all tags
     */
    public void fetchTags()
    {
        String host = HostPreferences.readSharedSetting(mContext, HostPreferences.HOST, "");
        String hash = HostPreferences.readSharedSetting(mContext, HostPreferences.HASH, "");
        new FetchTask(FetchTaskName.tags, hash).execute(host + "/api/v1/tags/");
    }

    public void createNote(Note note)
    {
        String host = HostPreferences.readSharedSetting(mContext, HostPreferences.HOST, "");
        String hash = HostPreferences.readSharedSetting(mContext, HostPreferences.HASH, "");
        new CreateNoteTask(note, hash, ModifyNote.create_note).execute(host + "/api/v1/notebooks/" + note.getNotebookId() + "/notes");
    }

    public void updateNote(Note note)
    {
        // FIXME will fail when try to update a shared note from another user
        String host = HostPreferences.readSharedSetting(mContext, HostPreferences.HOST, "");
        String hash = HostPreferences.readSharedSetting(mContext, HostPreferences.HASH, "");
        new CreateNoteTask(note, hash, ModifyNote.update_note).execute(host + "/api/v1/notebooks/" + note.getNotebookId() + "/notes/" + note.getId());
    }

    public void deleteNote(Note note)
    {
        // FIXME will fail when try to delete a shared note from another user

        String host = HostPreferences.readSharedSetting(mContext, HostPreferences.HOST, "");
        String hash = HostPreferences.readSharedSetting(mContext, HostPreferences.HASH, "");
        new CreateNoteTask(note, hash, ModifyNote.delete_note).execute(host + "/api/v1/notebooks/" + note.getNotebookId() + "/notes/" + note.getId());
    }

    private void parseAllNotebooks(String jsonStr)
    {
        List<Notebook> notebooks = new LinkedList<>();
        NotesDataSource notesDataSource = NotesDataSource.getInstance(mContext);
        notesDataSource.deleteAllNotes();

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

        storeAllNotebooks(notebooks);
    }

    private void parseAllNotes(String jsonStr)
    {
        JSONObject jsonData;
        try
        {
            jsonData = new JSONObject(jsonStr);

            JSONArray jsonNotes = jsonData.getJSONArray("response");

            for (int i = 0; i < jsonNotes.length(); i++)
            {
                JSONObject jsonNote = jsonNotes.getJSONObject(i);
                parseNote(jsonNote);
            }
        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG, "Error parsing JSON");
        }
    }

    private void parseNote(JSONObject jsonNote)
    {
        JSONObject version;
        try
        {
            version = jsonNote.getJSONObject("version");

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

            storeNote(note);
        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG, "Error parsing JSON " + jsonNote, e);
        }
    }

    private void storeNote(Note note)
    {
        NotesDataSource notesDataSource = NotesDataSource.getInstance(mContext);
        notesDataSource.createNote(note);
    }

    private void parseAllTags(String jsonStr)
    {
        List<Tag> tags = new LinkedList<>();

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
            Log.e(LOG_TAG, "Error parsing Json" + jsonStr);
        }

        storeAllTags(tags);
    }

    private void storeAllNotebooks(List<Notebook> notebooks)
    {
        NotesDataSource notesDataSource = NotesDataSource.getInstance(mContext);
        notesDataSource.deleteAllNotebooks();

        for (Notebook notebook : notebooks)
        {
            notesDataSource.createNotebook(notebook);
        }

        if (NotebooksFragment.getInstance() != null)
        {
            NotebooksFragment.getInstance().updateView();
        }
    }

    private void storeAllTags(List<Tag> tags)
    {
        NotesDataSource notesDataSource = NotesDataSource.getInstance(mContext);
        notesDataSource.deleteAllTags();

        for (Tag tag : tags)
        {
            notesDataSource.createTag(tag);
        }

        if (MainActivity.getInstance() != null)
        {
            MainActivity.getInstance().updateView();
        }
    }

    private void authenticationFailed()
    {
        Log.e(LOG_TAG, "Authentication failed");

        MainActivity.getInstance().logout();
    }

    private class FetchTask extends AsyncTask<String, Void, String>
    {
        private final FetchTaskName mFetchTask;
        private final String mHash;


        public FetchTask(FetchTaskName task, String hash)
        {
            mFetchTask = task;
            mHash = hash;
        }


        @Override
        protected String doInBackground(String... params)
        {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String jsonStr = "";

            try
            {
                URL url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Authorization", "Basic " + mHash);
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
            return jsonStr;
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);

            if (result.isEmpty())
            {
                Log.e(LOG_TAG, "Result is empty");
                return;
            }

            if (mFetchTask == FetchTaskName.notebooks)
            {
                parseAllNotebooks(result);
            }
            else if (mFetchTask == FetchTaskName.notes)
            {
                parseAllNotes(result);

                if (NotesFragment.getInstance() != null)
                {
                    NotesFragment.getInstance().updateView();
                }
            }
            else if (mFetchTask == FetchTaskName.tags)
            {
                parseAllTags(result);
            }
            else
            {
                Log.e(LOG_TAG, "Task does not exist");
            }
        }
    }

    private class CreateNoteTask extends AsyncTask<String, Void, String>
    {
        private final String mHash;
        private final Note mNote;
        private final ModifyNote mTask;

        public CreateNoteTask(Note note, String hash, ModifyNote task)
        {
            mNote = note;
            mHash = hash;
            mTask = task;
        }

        @Override
        protected String doInBackground(String... params)
        {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String jsonStr = "";

            try
            {
                URL url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestProperty("Authorization", "Basic " + mHash);
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(10000);

                // create json note
                JSONObject note = new JSONObject();
                note.put("title", mNote.getTitle());
                note.put("content", mNote.getContent());

                if (mTask == ModifyNote.create_note)
                {
                    urlConnection.setRequestMethod("POST");
                    note.put("content_preview", mNote.getPreview());
                }
                else if (mTask == ModifyNote.update_note)
                {
                    urlConnection.setRequestMethod("PUT");
                }
                else if (mTask == ModifyNote.delete_note)
                {
                    urlConnection.setRequestMethod("DELETE");
                }
                else
                {
                    Log.e(LOG_TAG, "Task does not exist");
                }

                urlConnection.connect();

                OutputStream outputStream = urlConnection.getOutputStream();
                outputStream.write(note.toString().getBytes());
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
                    authenticationFailed();
                }
                else if (responseCode != HttpURLConnection.HTTP_OK)
                {
                    Log.d(LOG_TAG, "Error while creating note, response code: " + urlConnection.getResponseCode());
                }
            }
            catch (JSONException e)
            {
                Log.e(LOG_TAG, "Error creating JSON");
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
                Log.e(LOG_TAG, "IOException", e);
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
            return jsonStr;
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);

            try
            {
                if (mTask == ModifyNote.create_note)
                {
                    JSONObject jsonNote = new JSONObject(result).getJSONObject("response");
                    parseNote(jsonNote);
                }
                else if (mTask == ModifyNote.update_note)
                {
                    JSONObject jsonNote = new JSONObject(result).getJSONObject("response");
                    Date date = DatabaseHelper.getDateTime(jsonNote.getString("updated_at"));
                    mNote.setUpdatedAt(date);
                    storeNote(mNote);
                }
                else if (mTask == ModifyNote.delete_note)
                {
                    NotesDataSource.getInstance(mContext).deleteNote(mNote);
                }

                if (NotesFragment.getInstance() != null)
                {
                    NotesFragment.getInstance().updateView();
                }
            }
            catch (JSONException e)
            {
                Log.e(LOG_TAG, "Error parsing JSON");
            }
        }
    }
}

