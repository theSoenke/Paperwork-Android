package rocks.paperwork.android.fragments;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import rocks.paperwork.android.R;
import rocks.paperwork.android.adapters.NotebookAdapter;
import rocks.paperwork.android.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.android.data.DatabaseContract;
import rocks.paperwork.android.data.NoteDataSource;
import rocks.paperwork.android.interfaces.AsyncCallback;
import rocks.paperwork.android.sync.SyncAdapter;


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
                bundle.putSerializable(NotesFragment.KEY_NOTEBOOK, notebook);
                fragment.setArguments(bundle);
                (getFragmentManager().beginTransaction().replace(R.id.main_container, fragment)).commit();
            }
        });

        FloatingActionButton addNotebook = (FloatingActionButton) view.findViewById(R.id.add_notebook);
        addNotebook.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // TODO enable creating notebooks when sync works
                //showNotebookCreateDialog();
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

    private void showNotebookCreateDialog()
    {
        final Notebook notebook = new Notebook(UUID.randomUUID().toString());
        final EditText userInput = new EditText(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.create_notebook_title))
                .setView(userInput)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        notebook.setTitle(userInput.getText().toString());
                        notebook.setSyncStatus(DatabaseContract.NotebookEntry.NOTEBOOK_STATUS.not_synced);
                        NoteDataSource.getInstance(getActivity()).insertNotebook(notebook);
                        SyncAdapter.syncImmediately(getActivity());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
