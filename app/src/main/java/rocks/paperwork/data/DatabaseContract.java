package rocks.paperwork.data;

import android.net.Uri;

/**
 * Defines table and column names for notes and notebooks
 */
class DatabaseContract
{
    public static final String CONTENT_AUTHORITY = "rocks.paperwork.app";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_NOTES = "notes";
    public static final String PATH_NOTEBOOKS = "notebooks";
    public static final String PATH_TAGS = "tags";

    public static final class NoteEntry
    {
        public static final String TABLE_NAME = "note";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_CONTENT = "content";
        public static final String COLUMN_UPDATED_AT = "updated_at";
        public static final String COLUMN_NOTEBOOK_KEY = "notebook_id";
    }

    public static final class NotebookEntry
    {
        public static final String TABLE_NAME = "notebook";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TITLE = "title";
    }

    public static final class TagEntry
    {
        public static final String TABLE_NAME = "tag";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TITLE = "title";
    }
}
