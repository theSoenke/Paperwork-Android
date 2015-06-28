package rocks.paperwork.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rocks.paperwork.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.adapters.NotesAdapter.Note;
import rocks.paperwork.adapters.Tag;

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
            int idColumn = cursor.getColumnIndex(DatabaseContract.NotebookEntry.COLUMN_ID);
            int titleColumn = cursor.getColumnIndex(DatabaseContract.NotebookEntry.COLUMN_TITLE);

            while (cursor.moveToNext())
            {
                Notebook notebook = new Notebook(cursor.getString(idColumn));
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

    public List<Note> getAllNotes()
    {
        List<Note> notes = new ArrayList<>();

        Cursor cursor = mContext.getContentResolver().query(
                DatabaseContract.NoteEntry.CONTENT_URI,
                null,
                null,
                null,
                DatabaseContract.NoteEntry.COLUMN_UPDATED_AT + " DESC");

        try
        {
            int idColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_ID);
            int titleColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_TITLE);
            int contentColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_CONTENT);
            int updatedAtColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_UPDATED_AT);
            int notebookColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY);

            while (cursor.moveToNext())
            {
                Note note = new Note(cursor.getString(idColumn));
                note.setTitle(cursor.getString(titleColumn));
                note.setContent(cursor.getString(contentColumn));
                Date date = DatabaseHelper.getDateTime(cursor.getString(updatedAtColumn));
                note.setUpdatedAt(date);
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
            int idColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_ID);
            int titleColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_TITLE);
            int contentColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_CONTENT);
            int updatedAtColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_UPDATED_AT);
            int notebookColumn = cursor.getColumnIndex(DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY);

            while (cursor.moveToNext())
            {
                Note note = new Note(cursor.getString(idColumn));
                note.setTitle(cursor.getString(titleColumn));
                note.setContent(cursor.getString(contentColumn));
                Date date = DatabaseHelper.getDateTime(cursor.getString(updatedAtColumn));
                note.setUpdatedAt(date);
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
            int idColumn = cursor.getColumnIndex(DatabaseContract.TagEntry.COLUMN_ID);
            int titleColumn = cursor.getColumnIndex(DatabaseContract.TagEntry.COLUMN_TITLE);

            while (cursor.moveToNext())
            {
                Tag tag = new Tag(cursor.getString(idColumn));
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
        values.put(DatabaseContract.NotebookEntry.COLUMN_ID, notebook.getId());
        values.put(DatabaseContract.NotebookEntry.COLUMN_TITLE, notebook.getTitle());

        mContext.getContentResolver().insert(DatabaseContract.NotebookEntry.CONTENT_URI, values);
    }

    public void insertNote(Note note)
    {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.NoteEntry.COLUMN_ID, note.getId());
        values.put(DatabaseContract.NoteEntry.COLUMN_TITLE, note.getTitle());
        values.put(DatabaseContract.NoteEntry.COLUMN_CONTENT, note.getContent());
        values.put(DatabaseContract.NoteEntry.COLUMN_UPDATED_AT, DatabaseHelper.getDateTime(note.getUpdatedAt()));
        values.put(DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY, note.getNotebookId());

        mContext.getContentResolver().insert(DatabaseContract.NoteEntry.CONTENT_URI, values);
    }

    public void insertTag(Tag tag)
    {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.NoteEntry.COLUMN_ID, tag.getId());
        values.put(DatabaseContract.TagEntry.COLUMN_TITLE, tag.getTitle());

        mContext.getContentResolver().insert(DatabaseContract.TagEntry.CONTENT_URI, values);
    }

    public void deleteAllNotebooks()
    {
        mContext.getContentResolver().delete(DatabaseContract.NotebookEntry.CONTENT_URI, null, null);
    }

    public void deleteNote(Note note)
    {
        String whereClause = DatabaseContract.NoteEntry.COLUMN_ID + " = '" + note.getId() + "'";
        mContext.getContentResolver().delete(
                DatabaseContract.NoteEntry.CONTENT_URI,
                whereClause,
                null);
    }

    public void deleteAllNotes()
    {
        mContext.getContentResolver().delete(DatabaseContract.NoteEntry.CONTENT_URI, null, null);
    }

    public void deleteAllTags()
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
