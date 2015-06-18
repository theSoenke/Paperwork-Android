package rocks.paperwork.fragments;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import rocks.paperwork.R;
import rocks.paperwork.activities.NoteActivity;
import rocks.paperwork.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.adapters.NotesAdapter;
import rocks.paperwork.adapters.NotesAdapter.Note;
import rocks.paperwork.data.NotesDataSource;
import rocks.paperwork.interfaces.AsyncCallback;
import rocks.paperwork.network.SyncNotesTask;


public class NotesFragment extends Fragment implements AsyncCallback
{
    private static NotesFragment sInstance;
    private NotesAdapter mNotesAdapter;
    private TextView emptyText;
    private SwipeRefreshLayout mSwipeContainer;


    public static NotesFragment getInstance()
    {
        return sInstance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState)
    {
        sInstance = this;
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        mNotesAdapter = new NotesAdapter(getActivity(), R.id.list_notebooks, new ArrayList<Note>());

        ListView notesList = (ListView) view.findViewById(R.id.list_notebooks);
        notesList.setAdapter(mNotesAdapter);

        FloatingActionButton addNote = (FloatingActionButton) view.findViewById(R.id.add_notebook);

        emptyText = (TextView) view.findViewById(R.id.empty);

        addNote.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showNotebookSelection();
            }
        });

        notesList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
            {
                Intent viewNoteIntent = new Intent(getActivity(), NoteActivity.class);
                viewNoteIntent.putExtra("NOTE", mNotesAdapter.getItem(position));
                viewNoteIntent.putExtra("IsEditable", false);
                startActivity(viewNoteIntent);
            }
        });

        mSwipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);

        // Setup refresh listener which triggers new data loading
        mSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                new SyncNotesTask(getActivity()).fetchAllData();
            }
        });

        // loads notes from the database
        updateData();

        return view;
    }

    @Override
    public void updateData()
    {
        mSwipeContainer.setRefreshing(false);
        mNotesAdapter.clear();

        NotesDataSource notesDataSource = NotesDataSource.getInstance(getActivity());
        List<Note> allNotes = notesDataSource.getAllNotes();

        mNotesAdapter.addAll(allNotes);
        mNotesAdapter.notifyDataSetChanged();

        if (mNotesAdapter.isEmpty())
        {
            emptyText.setVisibility(View.VISIBLE);
        }
        else
        {
            emptyText.setVisibility(View.GONE);
        }
    }

    private void showNotebookSelection()
    {
        NotesDataSource notesDataSource = NotesDataSource.getInstance(getActivity());
        final List<Notebook> allNotebooks = notesDataSource.getAllNotebooks();
        CharSequence[] notebookChars = new CharSequence[allNotebooks.size()];

        for (int i = 0; i < allNotebooks.size(); i++)
        {
            notebookChars[i] = allNotebooks.get(i).getTitle();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select a notebook").setIcon(R.mipmap.ic_notebook_grey)
                .setItems(notebookChars, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Intent editNoteIntent = new Intent(getActivity(), NoteActivity.class);
                        editNoteIntent.putExtra("NotebookId", allNotebooks.get(which).getId());
                        editNoteIntent.putExtra("IsEditable", true);
                        startActivity(editNoteIntent);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
