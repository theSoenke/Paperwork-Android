package rocks.paperwork.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import rocks.paperwork.data.DatabaseContract.NoteEntry;
import rocks.paperwork.data.DatabaseContract.NotebookEntry;
import rocks.paperwork.data.DatabaseContract.TagEntry;

public class DatabaseHelper extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 2;
    private static final String LOG_TAG = DatabaseHelper.class.getName();

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static Date getDateTime(String dateStr)
    {
        Date date = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try
        {
            date = dateFormat.parse(dateStr);
            date.getTime();
        }
        catch (ParseException e)
        {
            Log.e(LOG_TAG, "Error parsing date: " + dateStr);
        }

        return date;
    }

    public static String getDateTime(Date date)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(date);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase)
    {
        final String SQL_CREATE_NOTEBOOK_TABLE = "CREATE TABLE " + NotebookEntry.TABLE_NAME +
                " (" + NotebookEntry._ID + " TEXT PRIMARY KEY NOT NULL," +
                NotebookEntry.COLUMN_TITLE + " TEXT NOT NULL " +
                " );";

        final String SQL_CREATE_NOTE_TABLE = "CREATE TABLE " + NoteEntry.TABLE_NAME +
                " (" + NoteEntry._ID + " TEXT PRIMARY KEY NOT NULL," +
                NoteEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                NoteEntry.COLUMN_CONTENT + " TEXT NOT NULL, " +
                NoteEntry.COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                NoteEntry.COLUMN_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, " +
                NoteEntry.COLUMN_NOTEBOOK_KEY + " INTEGER NOT NULL, " +

                " FOREIGN KEY (" + NoteEntry.COLUMN_NOTEBOOK_KEY + ") REFERENCES " +
                NoteEntry.TABLE_NAME +
                " (" + NoteEntry._ID + ") " +
                " );";

        final String SQL_CREATE_TAG_TABLE = "CREATE TABLE " + TagEntry.TABLE_NAME +
                " (" + TagEntry._ID + " TEXT PRIMARY KEY NOT NULL," +
                TagEntry.COLUMN_TITLE + " TEXT NOT NULL " +
                " );";

        sqLiteDatabase.execSQL(SQL_CREATE_NOTEBOOK_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_NOTE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_TAG_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + NoteEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + NotebookEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TagEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
