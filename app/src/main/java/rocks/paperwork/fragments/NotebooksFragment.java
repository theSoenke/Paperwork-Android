package rocks.paperwork.fragments;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import rocks.paperwork.R;
import rocks.paperwork.adapters.NotebookAdapter;
import rocks.paperwork.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.data.NotesDataSource;
import rocks.paperwork.interfaces.AsyncCallback;


/**
 * A simple {@link Fragment} subclass.
 */
public class NotebooksFragment extends Fragment implements AsyncCallback
{
    private static NotebooksFragment sInstance;
    private NotebookAdapter mNotebooksAdapter;

    public static NotebooksFragment getsInstance()
    {
        return sInstance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        sInstance = this;
        View view = inflater.inflate(R.layout.fragment_notebooks, container, false);

        mNotebooksAdapter = new NotebookAdapter(getActivity(), R.id.list_notebooks, new ArrayList<Notebook>());

        ListView notesList = (ListView) view.findViewById(R.id.list_notebooks);
        notesList.setAdapter(mNotebooksAdapter);

        // loads notebooks from the database
        updateData();

        return view;
    }

    @Override
    public void updateData()
    {
        mNotebooksAdapter.clear();

        NotesDataSource notesDataSource = NotesDataSource.getInstance(getActivity());
        List<Notebook> allNotebooks = notesDataSource.getAllNotebooks();
        mNotebooksAdapter.addAll(allNotebooks);
    }
}
