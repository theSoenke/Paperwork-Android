package rocks.paperwork.android.sync.api;

import android.accounts.AuthenticatorException;
import android.content.Context;
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

import rocks.paperwork.android.adapters.NotesAdapter.Note;
import rocks.paperwork.android.adapters.Tag;
import rocks.paperwork.android.data.DatabaseContract;
import rocks.paperwork.android.data.DatabaseHelper;
import rocks.paperwork.android.data.NoteDataSource;

/**
 * Syncs notes with the server and parses note json
 */
public class NoteSync
{
    private static final String LOG_TAG = NoteSync.class.getSimpleName();

    /**
     * Creates a new note on the server
     */
    private static Note createNote(String host, String hash, Note note) throws IOException, JSONException, AuthenticatorException
    {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonStr;
        String path = host + "/api/v1/notebooks/" + note.getNotebookId() + "/notes";

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
            JSONObject jsonNote = new JSONObject();
            jsonNote.put("title", note.getTitle());
            jsonNote.put("content", note.getContent());
            jsonNote.put("content_preview", note.getPreview());

            OutputStream outputStream = urlConnection.getOutputStream();
            outputStream.write(jsonNote.toString().getBytes());
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
                return parseNote(jsonResponse.toString());
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

    /**
     * Updates a note on the server
     */
    private static boolean updateNote(String host, String hash, Note note) throws IOException, JSONException, AuthenticatorException
    {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonStr;
        String path = host + "/api/v1/notebooks/" + note.getNotebookId() + "/notes/" + note.getId();

        try
        {
            URL url = new URL(path);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Authorization", "Basic " + hash);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(15000);
            urlConnection.setRequestMethod("PUT");
            urlConnection.connect();

            // create json note
            JSONObject jsonNote = new JSONObject();
            jsonNote.put("title", note.getTitle());
            jsonNote.put("content", note.getContent());

            OutputStream outputStream = urlConnection.getOutputStream();
            outputStream.write(jsonNote.toString().getBytes());
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

                if (json.getBoolean("success"))
                {
                    return true;
                }
                else
                {
                    throw new ConnectException();
                }
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

    /**
     * Moves a note on the server to a different notebook
     */
    private static void moveNote(String host, String hash, Note note) throws IOException, JSONException, AuthenticatorException
    {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonStr;
        String path = host + "/api/v1/notebooks/" + note.getOldNotebookId() + "/notes/" + note.getId() + "/move/" + note.getNotebookId();

        try
        {
            URL url = new URL(path);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Authorization", "Basic " + hash);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(15000);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

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

    private static void deleteNote(String host, String hash, Note note) throws IOException, JSONException, AuthenticatorException
    {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonStr;
        String path = host + "/api/v1/notebooks/" + note.getNotebookId() + "/notes/" + note.getId();

        try
        {
            URL url = new URL(path);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Authorization", "Basic " + hash);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(15000);
            urlConnection.setRequestMethod("DELETE");
            urlConnection.connect();

            // create json note
            JSONObject jsonNote = new JSONObject();
            jsonNote.put("title", note.getTitle());
            jsonNote.put("content", note.getContent());

            OutputStream outputStream = urlConnection.getOutputStream();
            outputStream.write(jsonNote.toString().getBytes());
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

    /**
     * Uploads all new local notes
     */
    public static void uploadNotes(Context context, String host, String hash) throws IOException, JSONException, AuthenticatorException
    {
        NoteDataSource dataSource = NoteDataSource.getInstance(context);
        List<Note> notSyncedNotes = dataSource.getNotes(DatabaseContract.NoteEntry.NOTE_STATUS.not_synced);

        for (Note note : notSyncedNotes)
        {
            Note newNote = createNote(host, hash, note);
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
     * Updates notes on the server
     *
     * @param updatedNotes Notes that should be updated on the server
     */
    public static void updateNotes(Context context, String host, String hash, List<Note> updatedNotes) throws IOException, JSONException, AuthenticatorException
    {
        for (Note localNote : updatedNotes)
        {
            if (updateNote(host, hash, localNote))
            {
                localNote.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.synced);
                NoteDataSource.getInstance(context).updateNote(localNote);
            }
            else
            {
                Log.d(LOG_TAG, "Updating note failed");
            }
        }
    }

    /**
     * Updates notes on the server
     *
     * @param movedNotes Notes that should be moved to a different notebook on the server
     */

    public static void moveNotes(Context context, String host, String hash, List<Note> movedNotes) throws IOException, JSONException, AuthenticatorException
    {
        for (Note localNote : movedNotes)
        {
            moveNote(host, hash, localNote);
            localNote.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.synced);
            NoteDataSource.getInstance(context).updateNote(localNote);
        }
    }

    /**
     * Delete all local deleted notes on the server
     */
    public static void deleteNotes(Context context, String host, String hash) throws IOException, JSONException, AuthenticatorException
    {
        NoteDataSource dataSource = NoteDataSource.getInstance(context);
        List<Note> deletedNotes = dataSource.getNotes(DatabaseContract.NoteEntry.NOTE_STATUS.deleted);

        for (Note note : deletedNotes)
        {
            deleteNote(host, hash, note);
            dataSource.deleteNote(note);
        }
    }

    private static Note parseNote(String jsonStr) throws JSONException
    {
        JSONObject jsonNote = new JSONObject(jsonStr);
        JSONObject version = jsonNote.getJSONObject("version");

        String id = jsonNote.getString("id");
        String title = version.getString("title");
        String content = version.getString("content");
        Date date = DatabaseHelper.getDateTime(jsonNote.getString("updated_at"));
        String notebookId = jsonNote.getString("notebook_id");

        List<Tag> tags = new ArrayList<>();

        JSONArray jsonTags = jsonNote.getJSONArray("tags");

        for (int i = 0; i < jsonTags.length(); i++)
        {
            JSONObject jsonTag = jsonTags.getJSONObject(i);
            String tagId = jsonTag.getString("id");
            String tagTitle = jsonTag.getString("title");

            Tag tag = new Tag(tagId);
            tag.setTitle(tagTitle);

            tags.add(tag);
        }

        Note note = new Note(id);
        note.setNotebookId(notebookId);
        note.setTitle(title);
        note.setContent(content);
        note.setUpdatedAt(date);
        note.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.synced);
        note.setTags(tags);

        return note;
    }

    public static List<Note> parseNotes(String jsonStr) throws JSONException
    {
        List<Note> notes = new ArrayList<>();

        JSONObject jsonData;
        jsonData = new JSONObject(jsonStr);

        JSONArray jsonNotes = jsonData.getJSONArray("response");

        for (int i = 0; i < jsonNotes.length(); i++)
        {
            JSONObject jsonNote = jsonNotes.getJSONObject(i);
            Note note = parseNote(jsonNote.toString());

            notes.add(note);
        }

        return notes;
    }
}
