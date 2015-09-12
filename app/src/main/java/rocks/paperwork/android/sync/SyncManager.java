package rocks.paperwork.android.sync;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rocks.paperwork.android.adapters.NotesAdapter.Note;
import rocks.paperwork.android.data.DatabaseContract;

/**
 * Syncs local and remote version of objects
 */
public class SyncManager
{
    private final String LOG_TAG = SyncManager.class.getSimpleName();

    /**
     * Syncs local and remote notes. Upload new local notes first to the server before calling this method
     *
     * @param localNotes  Locally stored notes
     * @param remoteNotes Notes from the server
     */
    public NotesToSync syncNotes(List<Note> localNotes, List<Note> remoteNotes)
    {
        Map<String, Note> localNoteIds = new HashMap<>();
        Map<String, Note> remoteNoteIds = new HashMap<>();

        List<Note> updatedOnServer = new ArrayList<>();
        List<Note> noteUpdatesForServer = new ArrayList<>();
        List<Note> newRemoteNotes = new ArrayList<>();
        List<Note> localNotesToDelete = new ArrayList<>();
        List<Note> localMovedNotes = new ArrayList<>();


        for (Note localNote : localNotes)
        {
            localNoteIds.put(localNote.getId(), localNote);
        }

        for (Note remoteNote : remoteNotes)
        {
            remoteNoteIds.put(remoteNote.getId(), remoteNote);
        }

        for (String key : localNoteIds.keySet())
        {
            Note localNote = localNoteIds.get(key);

            // check whether local note is uploaded to the server and
            if (remoteNoteIds.containsKey(key))
            {
                // note exists on the server, get notes which need to synced
                Note remoteNote = remoteNoteIds.get(key);

                if (localNote.getSyncStatus() == DatabaseContract.NoteEntry.NOTE_STATUS.edited)
                {
                    if (localNote.getUpdatedAt().after(remoteNote.getUpdatedAt()))
                    {
                        // local note is newer
                        noteUpdatesForServer.add(localNote);

                        if (!localNote.getNotebookId().equals(remoteNote.getNotebookId()))
                        {
                            localMovedNotes.add(localNote);
                        }
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

                remoteNoteIds.remove(key);
            }
            else
            {
                // note is not available on server anymore and needs to be deleted
                localNotesToDelete.add(localNote);
            }
        }

        for (String key : remoteNoteIds.keySet())
        {
            Note remoteNote = remoteNoteIds.get(key);

            if (localNoteIds.containsKey(key))
            {
                Log.e(LOG_TAG, "Key should be already removed");
            }
            else
            {
                // note only exists on server
                newRemoteNotes.add(remoteNote);
            }
        }

        return new NotesToSync(noteUpdatesForServer, updatedOnServer, newRemoteNotes, localNotesToDelete, localMovedNotes);
    }

    public class NotesToSync
    {
        public final List<Note> locallyUpdatedNotes;
        public final List<Note> remoteUpdatedNotes;
        public final List<Note> remoteNewNotes;
        public final List<Note> localNotesToDelete;
        public final List<Note> localMovedNotes;

        public NotesToSync(List<Note> locallyUpdatedNotes,
                           List<Note> remoteUpdatedNotes,
                           List<Note> remoteNewNotes,
                           List<Note> localNotesToDelete,
                           List<Note> localMovedNotes)
        {
            this.locallyUpdatedNotes = locallyUpdatedNotes;
            this.remoteUpdatedNotes = remoteUpdatedNotes;
            this.remoteNewNotes = remoteNewNotes;
            this.localNotesToDelete = localNotesToDelete;
            this.localMovedNotes = localMovedNotes;
        }
    }
}