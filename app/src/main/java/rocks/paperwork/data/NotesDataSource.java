package rocks.paperwork.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rocks.paperwork.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.adapters.NotesAdapter.Note;
import rocks.paperwork.adapters.Tag;

/**
 * maintains database connection and gives access to notebooks, notes and tags
 */
public class NotesDataSource
{
    private static NotesDataSource sInstance;
    private final SQLiteDatabase mDatabase;

    private NotesDataSource(Context context)
    {
        mDatabase = new DatabaseHelper(context).getWritableDatabase();
    }

    public static NotesDataSource getInstance(Context context)
    {
        if (sInstance == null)
        {
            sInstance = new NotesDataSource(context);
        }
        return sInstance;
    }

    public List<Notebook> getAllNotebooks()
    {
        List<Notebook> notebooks = new ArrayList<>();

        Cursor cursor = mDatabase.query(DatabaseContract.NotebookEntry.TABLE_NAME, null, null, null, null, null, null);

        cursor.moveToFirst();

        while (!cursor.isAfterLast())
        {
            Notebook notebook = new Notebook(cursor.getString(0));
            notebook.setTitle(cursor.getString(1));
            notebooks.add(notebook);

            cursor.moveToNext();
        }

        cursor.close();

        return notebooks;
    }

    public List<Note> getAllNotes()
    {
        List<Note> notes = new ArrayList<>();

        Cursor cursor = mDatabase.query(
                DatabaseContract.NoteEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                DatabaseContract.NoteEntry.COLUMN_UPDATED_AT + " DESC");

        cursor.moveToFirst();

        while (!cursor.isAfterLast())
        {
            Note note = new Note(cursor.getString(0));
            note.setTitle(cursor.getString(1));
            note.setContent(cursor.getString(2));
            Date date = DatabaseHelper.getDateTime(cursor.getString(3));
            note.setUpdatedAt(date);
            note.setNotebookId(cursor.getString(4));
            notes.add(note);

//            Log.e("NoteId", note.getId());

            cursor.moveToNext();
        }

        cursor.close();

        return notes;
    }

    public List<Tag> getAllTags()
    {
        List<Tag> tags = new ArrayList<>();

        Cursor cursor = mDatabase.query(DatabaseContract.TagEntry.TABLE_NAME, null, null, null, null, null, null);

        cursor.moveToFirst();

        while (!cursor.isAfterLast())
        {
            Tag tag = new Tag(cursor.getString(0));
            tag.setTitle(cursor.getString(1));
            tags.add(tag);

            cursor.moveToNext();
        }

        cursor.close();

        return tags;
    }

    public void createNotebook(Notebook notebook)
    {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.NotebookEntry.COLUMN_ID, notebook.getId());
        values.put(DatabaseContract.NotebookEntry.COLUMN_TITLE, notebook.getTitle());

        mDatabase.insert(DatabaseContract.NotebookEntry.TABLE_NAME, null, values);
    }

    public void createNote(Note note)
    {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.NoteEntry.COLUMN_ID, note.getId());
        values.put(DatabaseContract.NoteEntry.COLUMN_TITLE, note.getTitle());
        values.put(DatabaseContract.NoteEntry.COLUMN_CONTENT, note.getContent());
        values.put(DatabaseContract.NoteEntry.COLUMN_UPDATED_AT, DatabaseHelper.getDateTime(note.getUpdatedAt()));
        values.put(DatabaseContract.NoteEntry.COLUMN_NOTEBOOK_KEY, note.getNotebookId());

        mDatabase.insert(DatabaseContract.NoteEntry.TABLE_NAME, null, values);
    }

    public void createTag(Tag tag)
    {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.NoteEntry.COLUMN_ID, tag.getId());
        values.put(DatabaseContract.TagEntry.COLUMN_TITLE, tag.getTitle());

        mDatabase.insert(DatabaseContract.TagEntry.TABLE_NAME, null, values);
    }

    public void deleteAllNotebooks()
    {
        mDatabase.delete(DatabaseContract.NotebookEntry.TABLE_NAME, null, null);
    }

    public void deleteNote(Note note)
    {
        String whereClause = DatabaseContract.NoteEntry.COLUMN_ID + " = '" + note.getId() + "'";
        mDatabase.delete(DatabaseContract.NoteEntry.TABLE_NAME,
                whereClause,
                null);
    }

    public void deleteAllNotes()
    {
        mDatabase.delete(DatabaseContract.NoteEntry.TABLE_NAME, null, null);
    }

    public void deleteAllTags()
    {
        mDatabase.delete(DatabaseContract.TagEntry.TABLE_NAME, null, null);
    }

    public void deleteAll()
    {
        deleteAllNotes();
        deleteAllTags();
        deleteAllNotebooks();
    }
}
