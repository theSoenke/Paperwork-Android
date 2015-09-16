package rocks.paperwork.android.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import rocks.paperwork.android.data.DatabaseContract.NoteEntry;
import rocks.paperwork.android.data.DatabaseContract.NoteTagsEntry;
import rocks.paperwork.android.data.DatabaseContract.NotebookEntry;
import rocks.paperwork.android.data.DatabaseContract.TagEntry;

public class DatabaseHelper extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 4;
    private static final String LOG_TAG = DatabaseHelper.class.getName();

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Converts date from a String to a Date
     *
     * @param dateStr Date in UTC TimeZone
     * @return Date in Locale TimeZone
     */
    public static Date getDateTime(String dateStr)
    {
        Date date = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try
        {
            date = dateFormat.parse(dateStr);
        }
        catch (ParseException e)
        {
            Log.e(LOG_TAG, "Error parsing date: " + dateStr);
        }

        return date;
    }

    public static String dateToString(Date date)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public static Date getCurrentTime()
    {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase)
    {
        final String SQL_CREATE_NOTEBOOK_TABLE = "CREATE TABLE " + NotebookEntry.TABLE_NAME +
                " (" + NotebookEntry._ID + " TEXT PRIMARY KEY NOT NULL," +
                NotebookEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                NotebookEntry.COLUMN_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0 " +
                " );";

        final String SQL_CREATE_NOTE_TABLE = "CREATE TABLE " + NoteEntry.TABLE_NAME +
                " (" + NoteEntry._ID + " TEXT PRIMARY KEY NOT NULL," +
                NoteEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                NoteEntry.COLUMN_CONTENT + " TEXT NOT NULL, " +
                NoteEntry.COLUMN_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                NoteEntry.COLUMN_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, " +
                NoteEntry.COLUMN_NOTEBOOK_KEY + " INTEGER NOT NULL, " +

                " FOREIGN KEY (" + NoteEntry.COLUMN_NOTEBOOK_KEY + ") REFERENCES " +
                NotebookEntry.TABLE_NAME + " (" + NotebookEntry._ID + ") " +
                " );";

        final String SQL_CREATE_TAG_TABLE = "CREATE TABLE " + TagEntry.TABLE_NAME +
                " (" + TagEntry._ID + " TEXT PRIMARY KEY NOT NULL," +
                TagEntry.COLUMN_TITLE + " TEXT NOT NULL " +
                " );";

        final String SQL_CREATE_TAGGED_NOTES_TABLE = "CREATE TABLE " + NoteTagsEntry.TABLE_NAME +
                " (" + NoteTagsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                NoteTagsEntry.COLUMN_NOTE_ID + " TEXT NOT NULL, " +
                NoteTagsEntry.COLUMN_TAG_ID + " TEXT NOT NULL, " +

                " FOREIGN KEY (" + NoteTagsEntry.COLUMN_NOTE_ID + ") REFERENCES " +
                NoteEntry.TABLE_NAME + " (" + NoteEntry._ID + "), " +
                " FOREIGN KEY (" + NoteTagsEntry.COLUMN_TAG_ID + ") REFERENCES " +
                TagEntry.TABLE_NAME + " (" + TagEntry._ID + ") " +
                " );";

        sqLiteDatabase.execSQL(SQL_CREATE_NOTEBOOK_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_NOTE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_TAG_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_TAGGED_NOTES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + NoteEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + NotebookEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TagEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + NoteTagsEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
