package rocks.paperwork.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import rocks.paperwork.R;
import rocks.paperwork.adapters.NotebookAdapter.Notebook;

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

        TextView notebookTitle = (TextView) rowView.findViewById(R.id.notebook_title);
        notebookTitle.setText(mNotebooks.get(position).getTitle());

        return rowView;
    }

    public static class Notebook
    {
        public static final String DEFAULT_ID = "00000000-0000-0000-0000-000000000000";
        private final String mId;
        private String mTitle;

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
    }
}
