package rocks.paperwork.android.adapters;


import android.content.Context;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rocks.paperwork.android.R;
import rocks.paperwork.android.data.DatabaseContract;

public class NotesAdapter extends ArrayAdapter<NotesAdapter.Note>
{
    private final Context mContext;
    private final List<Note> mNotes;

    public NotesAdapter(Context context, int resource, ArrayList<Note> notes)
    {
        super(context, resource, notes);
        mContext = context;
        mNotes = notes;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.note_item, parent, false);

        TextView noteTitle = (TextView) rowView.findViewById(R.id.note_title);
        TextView notePreview = (TextView) rowView.findViewById(R.id.note_preview);
        TextView noteDay = (TextView) rowView.findViewById(R.id.note_day);
        TextView noteMonth = (TextView) rowView.findViewById(R.id.note_month);
        TextView noteYear = (TextView) rowView.findViewById(R.id.note_year);

        String title = mNotes.get(position).getTitle();
        String preview = mNotes.get(position).getPreview();
        preview = Html.fromHtml(preview).toString();
        Date date = mNotes.get(position).getUpdatedAt();

        String day = (String) DateFormat.format("dd", date);
        String month = (String) DateFormat.format("MMM", date);
        String year = (String) DateFormat.format("yyyy", date);

        noteTitle.setText(title);
        noteTitle.setVisibility(title.length() == 0 ? View.GONE : View.VISIBLE);
        notePreview.setText(preview);
        noteDay.setText(day);
        noteMonth.setText(month);
        noteYear.setText(year);

        return rowView;
    }

    public static class Note implements Serializable
    {
        private String mId;
        private String mTitle;
        private String mContent;
        private String mNotebookId;
        private String mOldNotebookId;
        private Date mUpdatedAt;
        private DatabaseContract.NoteEntry.NOTE_STATUS mSyncStatus;
        private List<Tag> mTags = new ArrayList<>();

        public Note(String uuid)
        {
            mId = uuid;
        }

        public String getId()
        {
            return mId;
        }

        public String getTitle()
        {
            return mTitle;
        }

        public void setTitle(String title)
        {
            mTitle = title;
        }

        public String getContent()
        {
            return mContent;
        }

        public void setContent(String content)
        {
            mContent = content;
        }

        public String getPreview()
        {
            // takes first 255 characters for the preview
            return mContent.substring(0, mContent.length() >= 255 ? 255 : mContent.length());
        }

        @Override
        public int hashCode()
        {
            return getId().hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            return o instanceof Note && getId().equals(((Note) o).getId());
        }

        public String getNotebookId()
        {
            return mNotebookId;
        }

        public void setNotebookId(String id)
        {
            if (mNotebookId != null)
            {
                mOldNotebookId = mNotebookId;
            }
            mNotebookId = id;
        }

        public Date getUpdatedAt()
        {
            return mUpdatedAt;
        }

        public void setUpdatedAt(Date date)
        {
            mUpdatedAt = date;
        }

        public DatabaseContract.NoteEntry.NOTE_STATUS getSyncStatus()
        {
            return mSyncStatus;
        }

        public void setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS status)
        {
            mSyncStatus = status;
        }

        public String getOldNotebookId()
        {
            return mOldNotebookId;
        }

        public List<Tag> getTags()
        {
            return mTags;
        }

        public void setTags(List<Tag> tags)
        {
            mTags = tags;
        }
    }
}
