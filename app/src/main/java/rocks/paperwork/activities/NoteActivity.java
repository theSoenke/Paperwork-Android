package rocks.paperwork.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import rocks.paperwork.R;
import rocks.paperwork.adapters.NotesAdapter;
import rocks.paperwork.adapters.NotesAdapter.Note;
import rocks.paperwork.network.SyncNotesTask;

public class NoteActivity extends AppCompatActivity
{
    private final String LOG_TAG = NoteActivity.class.getName();
    private Toolbar mToolbar;
    private Note mNote;
    private EditText mTextTitle;
    private EditText mEditContent;
    private boolean mNewNote;
    private boolean mEditMode;
    private FloatingActionButton mEditNoteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);


        mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextTitle = (EditText) findViewById(R.id.note_title);
        mEditContent = (EditText) findViewById(R.id.note_edit_content);
        mEditNoteButton = (FloatingActionButton) findViewById(R.id.edit_note);

        mEditNoteButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                setMode(true);
            }
        });

        if (getIntent().hasExtra("NOTE"))
        {
            mNote = (NotesAdapter.Note) getIntent().getExtras().getSerializable("NOTE");
            mTextTitle.setText(mNote.getTitle());

            String content = mNote.getContent();
            mEditContent.setText(Html.fromHtml(content));
        }
        else
        {
            mNewNote = true;
        }

        if (getIntent().hasExtra("IsEditable"))
        {
            mEditMode = getIntent().getExtras().getBoolean("IsEditable");
        }
        else
        {
            Log.e(LOG_TAG, "IsEditable is not set");
        }

        setMode(mEditMode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.toolbar_text));
        }
    }

    /**
     * @param editMode enables or disables edit mode for the note
     */
    private void setMode(boolean editMode)
    {
        // will call onCreateOptionsMenu() and set visibility of save button
        invalidateOptionsMenu();

        mEditNoteButton.setVisibility(editMode ? View.GONE : View.VISIBLE);


        if (editMode)
        {
            mEditContent.requestFocus();
            showKeyboard();

            mEditContent.setFocusableInTouchMode(true);
            mEditContent.setCursorVisible(true);
            mTextTitle.setFocusableInTouchMode(true);
            mTextTitle.setCursorVisible(true);
            mToolbar.setNavigationIcon(R.drawable.ic_done_grey);
        }
        else
        {
            hideKeyboard();

            mEditContent.setFocusable(false);
            mEditContent.setCursorVisible(false);

            mTextTitle.setFocusable(false);
            mTextTitle.setCursorVisible(false);
            mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_grey);
        }

        mEditMode = editMode;
    }

    private void showKeyboard()
    {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void hideKeyboard()
    {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(mEditContent.getWindowToken(), 0);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home)
        {
            if (mEditMode)
            {
                String title = mTextTitle.getText().toString();

                if (title.isEmpty())
                {
                    mTextTitle.setError(getString(R.string.empty_title_error));
                    return false;
                }
            }

            if (mEditMode && !mNewNote)
            {
                setNoteResult();
                setMode(false);
            }
            else if (mEditMode && mNewNote)
            {
                setNoteResult();
                setMode(false);
                onBackPressed();
            }
            else
            {
                onBackPressed();
            }

            return true;
        }
        else if (id == R.id.action_delete)
        {
            SyncNotesTask deleteNoteTask = new SyncNotesTask(this);
            deleteNoteTask.deleteNote(mNote);
            onBackPressed();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause()
    {
        hideKeyboard();
        super.onPause();
    }

    private boolean changesToSave()
    {
        String title = mTextTitle.getText().toString();
        String content = mEditContent.getText().toString();

        if (mNote == null) // note does not exist yet
        {
            if (content.length() != 0 || title.length() != 0)
            {
                return true;
            }
        }
        else
        {
            if (!content.equals(Html.fromHtml(mNote.getContent()).toString()))
            {
                return true;
            }
            else if (!title.equals(mNote.getTitle()))
            {
                return true;
            }
        }
        return false;
    }

    private void setNoteResult()
    {
        if (changesToSave())
        {
            SyncNotesTask syncNotes = new SyncNotesTask(this);

            if (mNewNote)
            {
                mNote = new Note("");
                mNote.setTitle(mTextTitle.getText().toString());
                mNote.setContent(mEditContent.getText().toString());

                String notebookId = (String) getIntent().getExtras().getSerializable("NotebookId");
                mNote.setNotebookId(notebookId);
                syncNotes.createNote(mNote);
            }
            else
            {
                mNote.setTitle(mTextTitle.getText().toString());
                mNote.setContent(mEditContent.getText().toString());

                syncNotes.updateNote(mNote);
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        if (mEditMode)
        {
            setMode(false);
        }
        else if (changesToSave())
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.discard).setMessage(R.string.discard_changes)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            back();
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
        else
        {
            super.onBackPressed();
        }
    }

    private void back()
    {
        super.onBackPressed();
    }
}
