package rocks.paperwork.android.sync;

import android.content.Context;
import android.support.v4.util.Pair;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rocks.paperwork.android.adapters.NotesAdapter;
import rocks.paperwork.android.adapters.NotesAdapter.Note;
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
     */
    public NotesToSync syncNotes(List<Note> localNotes, List<Note> remoteNotes)
    {
        Map<String, Note> noteIds = new HashMap<>();
        List<Note> updatedOnServer = new ArrayList<>();
        List<Note> updatedLocally = new ArrayList<>();
        List<Note> newOnServer = new ArrayList<>();

        NoteDataSource dataSource = NoteDataSource.getInstance(mContext);

        for (Note remoteNote : remoteNotes)
        {
            noteIds.put(remoteNote.getId(), remoteNote);
            if (!localNotes.contains(remoteNote))
            {
                newOnServer.add(remoteNote);
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

                if (localNote.getSyncStatus() == DatabaseContract.NoteEntry.NOTE_STATUS.edited)
                {
                    if (localNote.getUpdatedAt().after(remoteNote.getUpdatedAt()))
                    {
                        // local note is newer
                        updatedLocally.add(localNote);
                    }
                }
                else if (localNote.getUpdatedAt().before(remoteNote.getUpdatedAt()))
                {
                    // remote note is newer
                    if (localNote.getSyncStatus() == DatabaseContract.NoteEntry.NOTE_STATUS.synced)
                    {
                        updatedOnServer.add(remoteNote);
                    }
                    else if (localNote.getSyncStatus() == DatabaseContract.NoteEntry.NOTE_STATUS.edited)
                    {
                        // FIXME note was edited locally and on the server -> conflict
                        Log.d(LOG_TAG, "Sync conflict");
                        // overwrite local note
                        updatedOnServer.add(remoteNote);
                    }
                }
            }
        }

        return new NotesToSync(updatedLocally, updatedOnServer, newOnServer);
    }

    public class NotesToSync
    {
        public final List<Note> locallyUpdatedNotes;
        public final List<Note> remoteUpdatedNotes;
        public final List<Note> remoteNewNotes;

        public NotesToSync(List<Note> locallyUpdatedNotes,
                           List<Note> remoteUpdatedNotes,
                           List<Note> remoteNewNotes)
        {
            this.locallyUpdatedNotes = locallyUpdatedNotes;
            this.remoteUpdatedNotes = remoteUpdatedNotes;
            this.remoteNewNotes = remoteNewNotes;
        }
    }
}