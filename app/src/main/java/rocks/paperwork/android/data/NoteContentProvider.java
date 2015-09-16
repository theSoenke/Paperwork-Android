package rocks.paperwork.android.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

public class NoteContentProvider extends ContentProvider
{
    private static final int NOTES = 100;
    private static final int NOTES_WITH_TAG = 101;
    private static final int NOTEBOOKS = 200;
    private static final int TAGS = 300;
    private static final int TAGS_OF_NOTE = 301;
    private static final int NOTE_TAGS = 400;
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final SQLiteQueryBuilder sTaggedNotesQueryBuilder;
    private static final SQLiteQueryBuilder sTagsOfNoteQueryBuilder;


    static
    {
        sTaggedNotesQueryBuilder = new SQLiteQueryBuilder();
        sTaggedNotesQueryBuilder.setTables(DatabaseContract.NoteEntry.TABLE_NAME + " INNER JOIN " +
                DatabaseContract.NoteTagsEntry.TABLE_NAME +
                " ON " + DatabaseContract.NoteTagsEntry.TABLE_NAME +
                "." + DatabaseContract.NoteTagsEntry.COLUMN_NOTE_ID +
                " = " + DatabaseContract.NoteEntry.TABLE_NAME +
                "." + DatabaseContract.NoteEntry._ID);

        sTagsOfNoteQueryBuilder = new SQLiteQueryBuilder();
        sTagsOfNoteQueryBuilder.setTables(DatabaseContract.TagEntry.TABLE_NAME + " INNER JOIN " +
                DatabaseContract.NoteTagsEntry.TABLE_NAME +
                " ON " + DatabaseContract.NoteTagsEntry.TABLE_NAME +
                "." + DatabaseContract.NoteTagsEntry.COLUMN_TAG_ID +
                " = " + DatabaseContract.TagEntry.TABLE_NAME +
                "." + DatabaseContract.TagEntry._ID);
    }

    private DatabaseHelper mOpenHelper;

    private static UriMatcher buildUriMatcher()
    {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DatabaseContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, DatabaseContract.PATH_NOTES, NOTES);
        matcher.addURI(authority, DatabaseContract.PATH_NOTES + "/*", NOTES_WITH_TAG);
        matcher.addURI(authority, DatabaseContract.PATH_NOTEBOOKS, NOTEBOOKS);
        matcher.addURI(authority, DatabaseContract.PATH_TAGS, TAGS);
        matcher.addURI(authority, DatabaseContract.PATH_TAGS + "/*", TAGS_OF_NOTE);
        matcher.addURI(authority, DatabaseContract.PATH_NOTE_TAGS, NOTE_TAGS);

        return matcher;
    }

    @Override
    public boolean onCreate()
    {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        Cursor cursor;


        switch (sUriMatcher.match(uri))
        {
            case NOTES:
            {
                cursor = mOpenHelper.getReadableDatabase().query(
                        DatabaseContract.NoteEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );

                break;
            }
            case  NOTES_WITH_TAG:
            {
                cursor = sTaggedNotesQueryBuilder.query(
                        mOpenHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            }
            case NOTEBOOKS:
            {
                cursor = mOpenHelper.getReadableDatabase().query(
                        DatabaseContract.NotebookEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case TAGS:
            {
                cursor = mOpenHelper.getReadableDatabase().query(
                        DatabaseContract.TagEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case TAGS_OF_NOTE:
            {
                cursor = sTagsOfNoteQueryBuilder.query(
                        mOpenHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri)
    {
        final int match = sUriMatcher.match(uri);

        switch (match)
        {
            case NOTES:
                return DatabaseContract.NoteEntry.CONTENT_TYPE;
            case  NOTES_WITH_TAG:
                return DatabaseContract.NoteEntry.CONTENT_TYPE;
            case NOTEBOOKS:
                return DatabaseContract.NotebookEntry.CONTENT_TYPE;
            case TAGS:
                return DatabaseContract.TagEntry.CONTENT_TYPE;
            case TAGS_OF_NOTE:
                return DatabaseContract.TagEntry.CONTENT_TYPE;
            case NOTE_TAGS:
                return DatabaseContract.NoteTagsEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues)
    {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match)
        {
            case NOTES:
            {
                long _id = db.insert(DatabaseContract.NoteEntry.TABLE_NAME, null, contentValues);
                if (_id > 0)
                {
                    returnUri = DatabaseContract.NoteEntry.buildNoteUri(_id);
                }
                else
                {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }

                break;
            }
            case NOTEBOOKS:
            {
                long _id = db.insert(DatabaseContract.NotebookEntry.TABLE_NAME, null, contentValues);
                if (_id > 0)
                {
                    returnUri = DatabaseContract.NotebookEntry.buildNotebookUri(_id);
                }
                else
                {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }
                break;
            }
            case TAGS:
            {
                long _id = db.insert(DatabaseContract.TagEntry.TABLE_NAME, null, contentValues);
                if (_id > 0)
                {
                    returnUri = DatabaseContract.TagEntry.buildTagUri(_id);
                }
                else
                {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }
                break;
            }
            case NOTE_TAGS:
            {
                long _id = db.insert(DatabaseContract.NoteTagsEntry.TABLE_NAME, null, contentValues);
                if (_id > 0)
                {
                    returnUri = DatabaseContract.NoteTagsEntry.buildTagUri(_id);
                }
                else
                {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int bulkInsert(Uri uri, @NonNull ContentValues[] values)
    {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int returnCount = 0;

        switch (match)
        {
            case NOTES:
            {
                db.beginTransaction();
                try
                {
                    for (ContentValues value : values)
                    {

                        long _id = db.replace(DatabaseContract.NoteEntry.TABLE_NAME, null, value);
                        if (_id != -1)
                        {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                }
                finally
                {
                    db.endTransaction();
                }
                break;
            }
            case NOTEBOOKS:
            {
                db.beginTransaction();
                try
                {
                    for (ContentValues value : values)
                    {
                        long _id = db.replace(DatabaseContract.NotebookEntry.TABLE_NAME, null, value);
                        if (_id != -1)
                        {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                }
                finally
                {
                    db.endTransaction();
                }
                break;
            }
            case TAGS:
            {
                db.beginTransaction();
                try
                {
                    for (ContentValues value : values)
                    {
                        long _id = db.replace(DatabaseContract.TagEntry.TABLE_NAME, null, value);
                        if (_id != -1)
                        {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                }
                finally
                {
                    db.endTransaction();
                }
                break;
            }
            case NOTE_TAGS:
            {
                db.beginTransaction();
                try
                {
                    for (ContentValues value : values)
                    {
                        long _id = db.replace(DatabaseContract.NoteTagsEntry.TABLE_NAME, null, value);
                        if (_id != -1)
                        {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                }
                finally
                {
                    db.endTransaction();
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return returnCount;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        // makes delete return the number of deleted rows
        if (selection == null)
        {
            selection = "1";
        }
        switch (match)
        {
            case NOTES:
            {
                rowsDeleted = db.delete(DatabaseContract.NoteEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case NOTEBOOKS:
            {
                rowsDeleted = db.delete(DatabaseContract.NotebookEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case TAGS:
            {
                rowsDeleted = db.delete(DatabaseContract.TagEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case NOTE_TAGS:
            {
                rowsDeleted = db.delete(DatabaseContract.NoteTagsEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs)
    {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match)
        {
            case NOTES:
            {
                rowsUpdated = db.update(DatabaseContract.NoteEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            }
            case NOTEBOOKS:
            {
                rowsUpdated = db.update(DatabaseContract.NotebookEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            }
            case TAGS:
            {
                rowsUpdated = db.update(DatabaseContract.TagEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}
