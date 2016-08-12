package rocks.paperwork.android.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines table and column names for notes and notebooks
 */
public class DatabaseContract
{
    public static final String CONTENT_AUTHORITY = "rocks.paperwork.app";
    public static final String PATH_NOTES = "notes";
    public static final String PATH_NOTEBOOKS = "notebooks";
    public static final String PATH_TAGS = "tags";
    public static final String PATH_NOTE_TAGS = "tagged_notes";
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static class NoteEntry implements BaseColumns
    {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_NOTES).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NOTES;
        public static final String TABLE_NAME = "note";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_CONTENT = "content";
        public static final String COLUMN_UPDATED_AT = "updated_at";
        public static final String COLUMN_SYNC_STATUS = "sync_status";
        public static final String COLUMN_NOTEBOOK_KEY = "notebook_id";

        public static Uri buildNoteUri(long id)
        {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildNoteWithTagUri()
        {
            return CONTENT_URI.buildUpon().appendPath(TagEntry.TABLE_NAME).build();
        }

        public enum NOTE_STATUS
        {
            not_synced,
            edited,
            synced,
            deleted
        }
    }

    public static final class NotebookEntry implements BaseColumns
    {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_NOTEBOOKS).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NOTEBOOKS;

        public static final String TABLE_NAME = "notebook";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_SYNC_STATUS = "sync_status";

        public static Uri buildNotebookUri(long id)
        {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public enum NOTEBOOK_STATUS
        {
            not_synced,
            edited,
            synced,
            deleted
        }
    }

    public static final class TagEntry implements BaseColumns
    {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_TAGS).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TAGS;

        public static final String TABLE_NAME = "tag";
        public static final String COLUMN_TITLE = "title";

        public static Uri buildTagUri(long id)
        {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildTagsFromNoteUri()
        {
            return CONTENT_URI.buildUpon().appendPath(NoteEntry.TABLE_NAME).build();
        }
    }

    public static final class NoteTagsEntry implements BaseColumns
    {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_NOTE_TAGS).build();
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NOTE_TAGS;

        public static final String TABLE_NAME = "note_tags";
        public static final String COLUMN_NOTE_ID = "note_id";
        public static final String COLUMN_TAG_ID = "tag_id";

        public static Uri buildTagUri(long id)
        {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
