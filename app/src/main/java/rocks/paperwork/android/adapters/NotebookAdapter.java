package rocks.paperwork.android.adapters;

import android.content.Context;
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
import rocks.paperwork.android.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.android.data.DatabaseContract;
import rocks.paperwork.android.data.NoteDataSource;

public class NotebookAdapter extends ArrayAdapter<Notebook>
{
    private final List<Notebook> mNotebooks;
    private final Context mContext;

    public NotebookAdapter(Context context, int resource, ArrayList<Notebook> notebooks)
    {
        super(context, resource, notebooks);
        mContext = context;
        mNotebooks = notebooks;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.notebook_item, parent, false);

        Notebook notebook = mNotebooks.get(position);

        int notebookCount = NoteDataSource.getInstance(mContext).getNumberOfNotesInNotebook(notebook);
        TextView notebookTitle = (TextView) rowView.findViewById(R.id.notebook_title);
        TextView notebookCountText = (TextView) rowView.findViewById(R.id.notebook_count);

        notebookTitle.setText(notebook.getTitle());

        String noteText;
        if (notebookCount == 0 || notebookCount > 1)
        {
            noteText = mContext.getString(R.string.notes);
        }
        else
        {
            noteText = mContext.getString(R.string.note);
        }
        notebookCountText.setText(Integer.toString(notebookCount) + " " + noteText);

        return rowView;
    }

    public static class Notebook implements Serializable
    {
        public static final String DEFAULT_ID = "00000000-0000-0000-0000-000000000000";
        private final String mId;
        private String mTitle;
        private Date mUpdatedAt;
        private DatabaseContract.NotebookEntry.NOTEBOOK_STATUS mSyncStatus;

        public Notebook(String id)
        {
            mId = id;
        }

        public String getTitle()
        {
            return mTitle;
        }

        public void setTitle(String title)
        {
            mTitle = title;
        }

        public String getId()
        {
            return mId;
        }

        public Date getUpdatedAt()
        {
            return mUpdatedAt;
        }

        public void setUpdatedAt(Date date)
        {
            mUpdatedAt = date;
        }

        public DatabaseContract.NotebookEntry.NOTEBOOK_STATUS getSyncStatus()
        {
            return mSyncStatus;
        }

        public void setSyncStatus(DatabaseContract.NotebookEntry.NOTEBOOK_STATUS status)
        {
            mSyncStatus = status;
        }

    }
}
