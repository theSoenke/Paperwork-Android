package rocks.paperwork.android.fragments;


import android.app.Fragment;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import rocks.paperwork.android.R;
import rocks.paperwork.android.adapters.NotebookAdapter;
import rocks.paperwork.android.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.android.data.DatabaseContract;
import rocks.paperwork.android.data.NoteDataSource;
import rocks.paperwork.android.interfaces.AsyncCallback;


/**
 * A simple {@link Fragment} subclass.
 */
public class NotebooksFragment extends Fragment implements AsyncCallback
{
    private NotebookAdapter mNotebooksAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_notebooks, container, false);

        mNotebooksAdapter = new NotebookAdapter(getActivity(), R.id.list_notebooks, new ArrayList<Notebook>());

        ListView notesList = (ListView) view.findViewById(R.id.list_notebooks);
        notesList.setAdapter(mNotebooksAdapter);

        notesList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                Notebook notebook = mNotebooksAdapter.getItem(i);
                getActivity().setTitle(notebook.getTitle());
                Fragment fragment = Fragment.instantiate(getActivity(), NotesFragment.class.getName());
                Bundle bundle = new Bundle();
                bundle.putSerializable("Notebook", notebook);
                fragment.setArguments(bundle);
                (getFragmentManager().beginTransaction().replace(R.id.main_container, fragment)).commit();
            }
        });

        getActivity().getContentResolver().registerContentObserver(
                DatabaseContract.NoteEntry.CONTENT_URI, true, new ContentObserver(new Handler(getActivity().getMainLooper()))
                {
                    @Override
                    public void onChange(boolean selfChange)
                    {
                        updateView();
                    }
                });


        // loads notebooks from the database
        updateView();

        return view;
    }

    @Override
    public void updateView()
    {
        mNotebooksAdapter.clear();

        NoteDataSource noteDataSource = NoteDataSource.getInstance(getActivity());
        List<Notebook> allNotebooks = noteDataSource.getAllNotebooks();
        mNotebooksAdapter.addAll(allNotebooks);
    }
}
