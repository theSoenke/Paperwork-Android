package rocks.paperwork.android.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rocks.paperwork.android.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.android.adapters.NotesAdapter.Note;
import rocks.paperwork.android.adapters.Tag;

/**
 * Abstraction layer for the NoteContentProvider
 */
public class NoteDataSource
{
    private static NoteDataSource sInstance;
    private final Context mContext;

    private NoteDataSource(Context context)
    {
        mContext = context;
    }

    public static NoteDataSource getInstance(Context context)
    {
        if (sInstance == null)
        {
            sInstance = new NoteDataSource(context);
        }
        return sInstance;
    }

    public List<Notebook> getAllNotebooks()
    {
        List<Notebook> notebooks = new ArrayList<>();
        Cursor cursor = mContext.getContentResolver().query(
                DatabaseContract.NotebookEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        try
        {
            int uuidColumn = cursor.getColumnIndex(DatabaseContract.NotebookEntry._ID);
            int titleColumn = cursor.getColumnIndex(DatabaseContract.NotebookEntry.COLUMN_TITLE);

            while (cursor.moveToNext())
            {
                Notebook notebook = new Notebook(cursor.getString(uuidColumn));
                notebook.setTitle(cursor.getString(titleColumn));
                notebooks.add(notebook);
            }
        }
        finally
        {
            cursor.close();
        }

        return notebooks;
    }

    public int getNumberOfNotesInNotebook(Notebook notebook)
    {
        String selection = DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY + " = '" + notebook.getId() + "'";

        Cursor cursor = mContext.getContentResolver().query(
                DatabaseContract.NoteEntry.CONTENT_URI,
                null,
                selection,
                null,
                null
        );

        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public List<Note> getNotes(DatabaseContract.NoteEntry.NOTE_STATUS selection)
    {
        List<Note> notes = new ArrayList<>();

        String whereClause;

        if (selection == null)
        {
            whereClause = DatabaseContract.NoteEntry.COLUMN_SYNC_STATUS + " != "
                    + DatabaseContract.NoteEntry.NOTE_STATUS.deleted.ordinal();
        }
        else
        {
            whereClause = DatabaseContract.NoteEntry.COLUMN_SYNC_STATUS + " = "
                    + selection.ordinal();
        }

        Cursor cursor = mContext.getContentResolver().query(
                DatabaseContract.NoteEntry.CONTENT_URI,
                null,
                whereClause,
                null,
                DatabaseContract.NoteEntry.COLUMN_UPDATED_AT + " DESC");

        try
        {
            int uuidColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry._ID);
            int titleColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_TITLE);
            int contentColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_CONTENT);
            int updatedAtColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_UPDATED_AT);
            int syncColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_SYNC_STATUS);
            int notebookColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY);

            while (cursor.moveToNext())
            {
                Note note = new Note(cursor.getString(uuidColumn));
                note.setTitle(cursor.getString(titleColumn));
                note.setContent(cursor.getString(contentColumn));
                Date date = DatabaseHelper.getDateTime(cursor.getString(updatedAtColumn));
                note.setUpdatedAt(date);
                note.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.values()[cursor.getInt(syncColumn)]);
                note.setNotebookId(cursor.getString(notebookColumn));
                note.setTags(getTagsOfNote(note));

                notes.add(note);
            }
        }
        finally
        {
            cursor.close();
        }
        return notes;
    }

    public List<Note> getNotesWithTag(Tag tag)
    {
        List<Note> notes = new ArrayList<>();

        String selection =
                DatabaseContract.NoteTagsEntry.TABLE_NAME +
                        "." + DatabaseContract.NoteTagsEntry.COLUMN_TAG_ID + " = ?";

        String[] selectionArgs = new String[]{tag.getId()};

        Cursor cursor = mContext.getContentResolver().query(
                DatabaseContract.NoteEntry.buildNoteWithTagUri(),
                null,
                selection,
                selectionArgs,
                null);

        try
        {
            int uuidColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry._ID);
            int titleColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_TITLE);
            int contentColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_CONTENT);
            int updatedAtColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_UPDATED_AT);
            int syncColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_SYNC_STATUS);
            int notebookColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY);

            while (cursor.moveToNext())
            {
                Note note = new Note(cursor.getString(uuidColumn));
                note.setTitle(cursor.getString(titleColumn));
                note.setContent(cursor.getString(contentColumn));
                Date date = DatabaseHelper.getDateTime(cursor.getString(updatedAtColumn));
                note.setUpdatedAt(date);
                note.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.values()[cursor.getInt(syncColumn)]);
                note.setNotebookId(cursor.getString(notebookColumn));
                note.setTags(getTagsOfNote(note));

                notes.add(note);
            }
        }
        finally
        {
            cursor.close();
        }
        return notes;
    }

    public List<Note> getAllNotesFromNotebook(Notebook notebook)
    {
        List<Note> notes = new ArrayList<>();
        String selection = DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY + " = '" + notebook.getId() + "'";

        Cursor cursor = mContext.getContentResolver().query(
                DatabaseContract.NoteEntry.CONTENT_URI,
                null,
                selection,
                null,
                DatabaseContract.NoteEntry.COLUMN_UPDATED_AT + " DESC");

        try
        {
            int uuidColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry._ID);
            int titleColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_TITLE);
            int contentColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_CONTENT);
            int updatedAtColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_UPDATED_AT);
            int syncColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_SYNC_STATUS);
            int notebookColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY);

            while (cursor.moveToNext())
            {
                Note note = new Note(cursor.getString(uuidColumn));
                note.setTitle(cursor.getString(titleColumn));
                note.setContent(cursor.getString(contentColumn));
                Date date = DatabaseHelper.getDateTime(cursor.getString(updatedAtColumn));
                note.setUpdatedAt(date);
                note.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.values()[cursor.getInt(syncColumn)]);
                note.setNotebookId(cursor.getString(notebookColumn));
                note.setTags(getTagsOfNote(note));

                notes.add(note);
            }
        }
        finally
        {
            cursor.close();
        }

        return notes;
    }

    public List<Tag> getAllTags()
    {
        List<Tag> tags = new ArrayList<>();

        Cursor cursor = mContext.getContentResolver().query(
                DatabaseContract.TagEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        try
        {
            int uuidColumn = cursor.getColumnIndex(DatabaseContract.TagEntry._ID);
            int titleColumn = cursor.getColumnIndex(DatabaseContract.TagEntry.COLUMN_TITLE);

            while (cursor.moveToNext())
            {
                Tag tag = new Tag(cursor.getString(uuidColumn));
                tag.setTitle(cursor.getString(titleColumn));
                tags.add(tag);
            }
        }
        finally
        {
            cursor.close();
        }

        return tags;
    }

    public List<Tag> getTagsOfNote(Note note)
    {
        List<Tag> tags = new ArrayList<>();

        String selection =
                DatabaseContract.NoteTagsEntry.TABLE_NAME +
                        "." + DatabaseContract.NoteTagsEntry.COLUMN_NOTE_ID + " = ?";

        String[] selectionArgs = new String[]{note.getId()};

        Cursor cursor = mContext.getContentResolver().query(
                DatabaseContract.TagEntry.buildTagsFromNoteUri(),
                null,
                selection,
                selectionArgs,
                null);

        try
        {
            int uuidColumn = cursor.getColumnIndex(DatabaseContract.TagEntry._ID);
            int titleColumn = cursor.getColumnIndex(DatabaseContract.TagEntry.COLUMN_TITLE);

            while (cursor.moveToNext())
            {
                Tag tag = new Tag(cursor.getString(uuidColumn));
                tag.setTitle(cursor.getString(titleColumn));
                tags.add(tag);
            }
        }
        finally
        {
            cursor.close();
        }

        return tags;
    }


    public void insertNotebook(Notebook notebook)
    {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.NotebookEntry._ID, notebook.getId());
        values.put(DatabaseContract.NotebookEntry.COLUMN_TITLE, notebook.getTitle());
        values.put(DatabaseContract.NotebookEntry.COLUMN_SYNC_STATUS, notebook.getSyncStatus().ordinal());

        mContext.getContentResolver().insert(DatabaseContract.NotebookEntry.CONTENT_URI, values);
    }

    public void bulkInsertNotebooks(List<Notebook> notebooks)
    {
        ContentValues[] contentValues = new ContentValues[notebooks.size()];

        for (int i = 0; i < notebooks.size(); i++)
        {
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.NotebookEntry._ID, notebooks.get(i).getId());
            values.put(DatabaseContract.NotebookEntry.COLUMN_TITLE, notebooks.get(i).getTitle());

            contentValues[i] = values;
        }

        mContext.getContentResolver().bulkInsert(DatabaseContract.NotebookEntry.CONTENT_URI, contentValues);
    }

    public void insertNote(Note note)
    {
        ContentValues values = new ContentValues();

        values.put(DatabaseContract.NoteEntry._ID, note.getId());
        values.put(DatabaseContract.NoteEntry.COLUMN_TITLE, note.getTitle());
        values.put(DatabaseContract.NoteEntry.COLUMN_CONTENT, note.getContent());
        values.put(DatabaseContract.NoteEntry.COLUMN_UPDATED_AT, DatabaseHelper.dateToString(note.getUpdatedAt()));
        values.put(DatabaseContract.NoteEntry.COLUMN_SYNC_STATUS, note.getSyncStatus().ordinal());
        values.put(DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY, note.getNotebookId());

        mContext.getContentResolver().insert(DatabaseContract.NoteEntry.CONTENT_URI, values);
    }

    public void updateNote(Note note)
    {
        String selection = DatabaseContract.NoteEntry._ID + " = '" + note.getId() + "'";

        ContentValues values = new ContentValues();

        values.put(DatabaseContract.NoteEntry._ID, note.getId());
        values.put(DatabaseContract.NoteEntry.COLUMN_TITLE, note.getTitle());
        values.put(DatabaseContract.NoteEntry.COLUMN_CONTENT, note.getContent());
        values.put(DatabaseContract.NoteEntry.COLUMN_UPDATED_AT, DatabaseHelper.dateToString(note.getUpdatedAt()));
        values.put(DatabaseContract.NoteEntry.COLUMN_SYNC_STATUS, note.getSyncStatus().ordinal());
        values.put(DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY, note.getNotebookId());

        mContext.getContentResolver().update(DatabaseContract.NoteEntry.CONTENT_URI, values, selection, null);
    }

    public void bulkInsertNotes(List<Note> notes)
    {
        ContentValues[] contentValues = new ContentValues[notes.size()];

        for (int i = 0; i < notes.size(); i++)
        {
            Note note = notes.get(i);

            ContentValues values = new ContentValues();
            values.put(DatabaseContract.NoteEntry._ID, note.getId());
            values.put(DatabaseContract.NoteEntry.COLUMN_TITLE, note.getTitle());
            values.put(DatabaseContract.NoteEntry.COLUMN_CONTENT, note.getContent());
            values.put(DatabaseContract.NoteEntry.COLUMN_UPDATED_AT, DatabaseHelper.dateToString(note.getUpdatedAt()));
            values.put(DatabaseContract.NoteEntry.COLUMN_SYNC_STATUS, note.getSyncStatus().ordinal());
            values.put(DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY, note.getNotebookId());

            contentValues[i] = values;
        }

        mContext.getContentResolver().bulkInsert(DatabaseContract.NoteEntry.CONTENT_URI, contentValues);
    }

    public void insertTaggedNote(Tag tag, Note note)
    {
        ContentValues values = new ContentValues();

        values.put(DatabaseContract.NoteTagsEntry.COLUMN_NOTE_ID, note.getId());
        values.put(DatabaseContract.NoteTagsEntry.COLUMN_TAG_ID, tag.getId());

        mContext.getContentResolver().insert(DatabaseContract.NoteTagsEntry.CONTENT_URI, values);
    }

    public List<Note> searchNotes(String query)
    {
        // TODO use FTS for search

        List<Note> notes = new ArrayList<>();
        String selection = DatabaseContract.NoteEntry.COLUMN_CONTENT + " LIKE '%" + query + "%'"
                + "OR " + DatabaseContract.NoteEntry.COLUMN_TITLE + " LIKE '%" + query + "%'";

        Cursor cursor = mContext.getContentResolver().query(DatabaseContract.NoteEntry.CONTENT_URI,
                null,
                selection,
                null,
                null);

        try
        {
            int uuidColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry._ID);
            int titleColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_TITLE);
            int contentColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_CONTENT);
            int updatedAtColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_UPDATED_AT);
            int syncColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_SYNC_STATUS);
            int notebookColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY);

            while (cursor.moveToNext())
            {
                Note note = new Note(cursor.getString(uuidColumn));
                note.setTitle(cursor.getString(titleColumn));
                note.setContent(cursor.getString(contentColumn));
                Date date = DatabaseHelper.getDateTime(cursor.getString(updatedAtColumn));
                note.setUpdatedAt(date);
                note.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.values()[cursor.getInt(syncColumn)]);
                note.setNotebookId(cursor.getString(notebookColumn));
                notes.add(note);
            }
        }
        finally
        {
            cursor.close();
        }
        return notes;
    }

    public void insertTag(Tag tag)
    {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.TagEntry._ID, tag.getId());
        values.put(DatabaseContract.TagEntry.COLUMN_TITLE, tag.getTitle());

        mContext.getContentResolver().insert(DatabaseContract.TagEntry.CONTENT_URI, values);
    }

    public void bulkInsertTags(List<Tag> tags)
    {
        ContentValues[] contentValues = new ContentValues[tags.size()];

        for (int i = 0; i < tags.size(); i++)
        {
            ContentValues values = new ContentValues();
            values.put(DatabaseContract.NotebookEntry._ID, tags.get(i).getId());
            values.put(DatabaseContract.NotebookEntry.COLUMN_TITLE, tags.get(i).getTitle());

            contentValues[i] = values;
        }

        mContext.getContentResolver().bulkInsert(DatabaseContract.TagEntry.CONTENT_URI, contentValues);
    }

    public void deleteNote(Note note)
    {
        String whereClause = DatabaseContract.NoteEntry._ID + " = '" + note.getId() + "'";
        mContext.getContentResolver().delete(
                DatabaseContract.NoteEntry.CONTENT_URI,
                whereClause,
                null);
    }

    public void deleteTagsOfNote(Note note)
    {
        String whereClause = DatabaseContract.NoteTagsEntry.COLUMN_NOTE_ID + " = '" + note.getId() + "'";

        mContext.getContentResolver().delete(DatabaseContract.NoteTagsEntry.CONTENT_URI,
                whereClause,
                null);
    }

    private void deleteAllNotes()
    {
        mContext.getContentResolver().delete(DatabaseContract.NoteEntry.CONTENT_URI, null, null);
    }

    private void deleteAllNotebooks()
    {
        mContext.getContentResolver().delete(DatabaseContract.NotebookEntry.CONTENT_URI, null, null);
    }

    private void deleteAllTags()
    {
        mContext.getContentResolver().delete(DatabaseContract.TagEntry.CONTENT_URI, null, null);
    }

    public void deleteAll()
    {
        deleteAllNotes();
        deleteAllTags();
        deleteAllNotebooks();
    }
}
