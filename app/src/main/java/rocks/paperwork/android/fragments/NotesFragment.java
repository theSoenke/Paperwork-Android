package rocks.paperwork.android.fragments;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
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

import rocks.paperwork.android.R;
import rocks.paperwork.android.activities.NoteActivity;
import rocks.paperwork.android.adapters.NotebookAdapter.Notebook;
import rocks.paperwork.android.adapters.NotesAdapter;
import rocks.paperwork.android.adapters.NotesAdapter.Note;
import rocks.paperwork.android.adapters.Tag;
import rocks.paperwork.android.data.DatabaseContract;
import rocks.paperwork.android.data.DatabaseHelper;
import rocks.paperwork.android.data.NoteDataSource;
import rocks.paperwork.android.interfaces.AsyncCallback;
import rocks.paperwork.android.sync.SyncAdapter;


public class NotesFragment extends Fragment implements AsyncCallback
{
    public static final String KEY_NOTEBOOK = "notebook";
    public static final String KEY_SEARCH_MODE = "search_mode";
    public static final String KEY_TAG = "tag";
    private NotesAdapter mNotesAdapter;
    private TextView emptyText;
    private SwipeRefreshLayout mSwipeContainer;
    private Notebook mNotebook;
    private Tag mTag;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        mNotesAdapter = new NotesAdapter(getActivity(), R.id.list_notes, new ArrayList<Note>());

        ListView notesList = (ListView) view.findViewById(R.id.list_notes);
        notesList.setAdapter(mNotesAdapter);

        FloatingActionButton addNote = (FloatingActionButton) view.findViewById(R.id.add_note);

        emptyText = (TextView) view.findViewById(R.id.empty);

        addNote.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mNotebook == null)
                {
                    showNotebookSelection();
                }
                else
                {
                    createNewNote(mNotebook);
                }
            }
        });

        notesList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
            {
                viewNote(mNotesAdapter.getItem(position), false);
            }
        });

        notesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                showNoteDialog(i);
                return true;
            }
        });

        mSwipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);

        // Setup refresh listener which triggers new data loading
        mSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                SyncAdapter.syncImmediately(getActivity(), mSwipeContainer);
            }
        });

        boolean isSearchMode = false;
        Bundle bundle = getArguments();
        if (bundle != null)
        {
            if (bundle.containsKey(KEY_NOTEBOOK))
            {
                mNotebook = (Notebook) bundle.getSerializable(KEY_NOTEBOOK);
            }

            if (bundle.containsKey(KEY_SEARCH_MODE))
            {
                isSearchMode = bundle.getBoolean(KEY_SEARCH_MODE);
                addNote.setVisibility(isSearchMode ? View.GONE : View.VISIBLE);
            }

            if (bundle.containsKey(KEY_TAG))
            {
                mTag = (Tag) bundle.getSerializable(KEY_TAG);
            }
        }

        getActivity().getContentResolver().registerContentObserver(
                DatabaseContract.NoteEntry.CONTENT_URI, true, new ContentObserver(new Handler(getActivity().getMainLooper()))
                {
                    @Override
                    public void onChange(boolean selfChange)
                    {
                        updateView();
                    }
                });

        if (isSearchMode)
        {
            mSwipeContainer.setEnabled(false);
        }
        else
        {
            updateView();
        }

        return view;
    }

    @Override
    public void updateView()
    {
        mSwipeContainer.setRefreshing(false);
        mNotesAdapter.clear();

        NoteDataSource noteDataSource = NoteDataSource.getInstance(getActivity());
        List<Note> notes;

        if (mNotebook != null)
        {
            notes = noteDataSource.getAllNotesFromNotebook(mNotebook);
        }
        else if (mTag != null)
        {
            notes = noteDataSource.getNotesWithTag(mTag);
        }
        else
        {
            notes = noteDataSource.getNotes(null);
        }

        mNotesAdapter.addAll(notes);

        if (mNotesAdapter.isEmpty())
        {
            emptyText.setVisibility(View.VISIBLE);
        }
        else
        {
            emptyText.setVisibility(View.GONE);
        }
    }

    public void searchNotes(String query)
    {
        if (!query.isEmpty())
        {
            mNotesAdapter.clear();
            List<Note> searchResult = NoteDataSource.getInstance(getActivity()).searchNotes(query);
            mNotesAdapter.addAll(searchResult);
        }
    }

    private void showNotebookSelection()
    {
        NoteDataSource noteDataSource = NoteDataSource.getInstance(getActivity());
        final List<Notebook> allNotebooks = noteDataSource.getAllNotebooks();
        CharSequence[] notebookChars = new CharSequence[allNotebooks.size()];

        for (int i = 0; i < allNotebooks.size(); i++)
        {
            notebookChars[i] = allNotebooks.get(i).getTitle();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.notebook_selection_title).setIcon(R.drawable.ic_notebook_grey)
                .setItems(notebookChars, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        createNewNote(allNotebooks.get(which));
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createNewNote(Notebook notebook)
    {
        Intent editNoteIntent = new Intent(getActivity(), NoteActivity.class);
        editNoteIntent.putExtra(NoteActivity.KEY_NOTEBOOK_ID, notebook.getId());
        editNoteIntent.putExtra(NoteActivity.KEY_EDIT_MODE, true);
        startActivity(editNoteIntent);
    }

    private void viewNote(Note note, boolean editMode)
    {
        Intent viewNoteIntent = new Intent(getActivity(), NoteActivity.class);
        viewNoteIntent.putExtra(NoteActivity.KEY_NOTE, note);
        viewNoteIntent.putExtra(NoteActivity.KEY_EDIT_MODE, editMode);
        startActivity(viewNoteIntent);
    }

    private void showNoteDialog(int position)
    {
        final Note note = mNotesAdapter.getItem(position);
        CharSequence[] options = {
                getString(R.string.edit),
                getString(R.string.share_note),
                getString(R.string.add_tag),
                getString(R.string.move_to),
                getString(R.string.delete_title)};

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(options, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                switch (which)
                {
                    case 0: // edit
                        viewNote(note, true);
                        break;
                    case 1: // share
                        // TODO implement share
                        break;
                    case 2: // add a tag
                        // TODO implement add tags
                        break;
                    case 3: // Move To
                        showMoveNoteDialog(note);
                        break;
                    case 4: // delete note
                        showDeleteNoteDialog(note);
                        break;
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showMoveNoteDialog(final Note note)
    {
        NoteDataSource noteDataSource = NoteDataSource.getInstance(getActivity());
        final List<Notebook> allNotebooks = noteDataSource.getAllNotebooks();
        CharSequence[] notebookChars = new CharSequence[allNotebooks.size()];

        for (int i = 0; i < allNotebooks.size(); i++)
        {
            notebookChars[i] = allNotebooks.get(i).getTitle();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.move_to)).setIcon(R.drawable.ic_notebook_grey)
                .setItems(notebookChars, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Notebook notebook = allNotebooks.get(which);

                        if (!note.getNotebookId().equals(notebook.getId()))
                        {
                            note.setNotebookId(notebook.getId());
                            note.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.edited);
                            note.setUpdatedAt(DatabaseHelper.getCurrentTime());
                            NoteDataSource.getInstance(getActivity()).updateNote(note);
                            SyncAdapter.syncImmediately(getActivity());
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteNoteDialog(final Note note)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.delete_title) + ": " + note.getTitle())
                .setMessage(R.string.delete_note_message)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        note.setSyncStatus(DatabaseContract.NoteEntry.NOTE_STATUS.deleted);
                        NoteDataSource.getInstance(getActivity()).updateNote(note);
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
