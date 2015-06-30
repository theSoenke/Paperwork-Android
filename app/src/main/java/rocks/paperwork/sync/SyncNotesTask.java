package rocks.paperwork.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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

import rocks.paperwork.activities.MainActivity;
import rocks.paperwork.adapters.NotesAdapter.Note;
import rocks.paperwork.data.HostPreferences;
import rocks.paperwork.data.NoteDataSource;
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

    public void deleteNote(Note note)
    {
        // FIXME will fail when try to delete a shared note from another user
        String host = HostPreferences.readSharedSetting(mContext, HostPreferences.HOST, "");
        String hash = HostPreferences.readSharedSetting(mContext, HostPreferences.HASH, "");
        new ModifyNoteTask(note, hash, ModifyNote.delete_note).execute(host + "/api/v1/notebooks/" + note.getNotebookId() + "/notes/" + note.getId());
    }

    private void authenticationFailed()
    {
        Log.e(LOG_TAG, "Authentication failed");

        MainActivity.getInstance().logout();
    }

    private enum ModifyNote
    {
        create_note,
        update_note,
        delete_note
    }

    private class ModifyNoteTask extends AsyncTask<String, Void, String>
    {
        private final String mHash;
        private final Note mNote;
        private final ModifyNote mTask;

        public ModifyNoteTask(Note note, String hash, ModifyNote task)
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

                if (mTask != ModifyNote.delete_note)
                {
                    OutputStream outputStream = urlConnection.getOutputStream();
                    outputStream.write(note.toString().getBytes());
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

            if (mTask == ModifyNote.delete_note)
            {
                NoteDataSource.getInstance(mContext).deleteNote(mNote);
            }

            if (NotesFragment.getInstance() != null)
            {
                NotesFragment.getInstance().updateView();
            }
        }
    }
}

