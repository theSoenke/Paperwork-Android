package rocks.paperwork.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;

import rocks.paperwork.adapters.NotebookAdapter.Notebook;

/**
 * Defines table and column names for notes and notebooks
 */
public class DatabaseContract
{
    public static final String CONTENT_AUTHORITY = "rocks.paperwork.app";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_NOTES = "notes";
    public static final String PATH_NOTEBOOKS = "notebooks";
    public static final String PATH_TAGS = "tags";

    public static final class NoteEntry
    {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_NOTES).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NOTES;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NOTES;

        public static final String TABLE_NAME = "note";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_CONTENT = "content";
        public static final String COLUMN_UPDATED_AT = "updated_at";
        public static final String COLUMN_NOTEBOOK_KEY = "notebook_id";

        public static Uri buildNoteUri(long id)
        {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildNotesFromNotebook(Notebook notebook)
        {
            return CONTENT_URI.buildUpon().appendPath(PATH_NOTES).appendPath(notebook.getId()).build();
        }
    }

    public static final class NotebookEntry
    {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_NOTEBOOKS).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NOTEBOOKS;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NOTEBOOKS;

        public static final String TABLE_NAME = "notebook";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TITLE = "title";

        public static Uri buildNotebookUri(long id)
        {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class TagEntry
    {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_TAGS).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TAGS;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TAGS;

        public static final String TABLE_NAME = "tag";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TITLE = "title";

        public static Uri buildTagUri(long id)
        {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
