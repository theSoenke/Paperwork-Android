package rocks.paperwork.android.sync;

import android.content.Context;
import android.support.v4.util.Pair;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rocks.paperwork.android.adapters.NotesAdapter;
import rocks.paperwork.android.data.DatabaseContract;
import rocks.paperwork.android.data.NoteDataSource;

/**
 * Syncs local and remote version of objects
 */
public class SyncManager
{
    private final String LOG_TAG = SyncManager.class.getSimpleName();
    private final Context mContext;

    public SyncManager(Context context)
    {
        mContext = context;
    }

    /**
     * Syncs local and remote notes
     *
     * @param localNotes  Locally stored notes
     * @param remoteNotes Notes from the server
     * @return First list contains notes that need to be updated locally, second list of notes need to be updated on the server
     */
    public Pair<List<NotesAdapter.Note>, List<NotesAdapter.Note>> syncNotes(List<NotesAdapter.Note> localNotes, List<NotesAdapter.Note> remoteNotes)
    {
        Map<String, NotesAdapter.Note> noteIds = new HashMap<>();
        List<NotesAdapter.Note> updatedNotes = new ArrayList<>();
        List<NotesAdapter.Note> updateOnServer = new ArrayList<>();

        NoteDataSource dataSource = NoteDataSource.getInstance(mContext);

        for (NotesAdapter.Note remoteNote : remoteNotes)
        {
            noteIds.put(remoteNote.getId(), remoteNote);
            if (!localNotes.contains(remoteNote))
            {
                updatedNotes.add(remoteNote);
            }
        }

        for (NotesAdapter.Note localNote : localNotes)
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
                NotesAdapter.Note remoteNote = noteIds.get(localNote.getId());

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
                        // overwrite local note
                        updatedNotes.add(remoteNote);
                    }
                }
            }
        }

        return new Pair<>(updatedNotes, updateOnServer);
    }
}