package rocks.paperwork.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import rocks.paperwork.R;
import rocks.paperwork.adapters.NotesAdapter;
import rocks.paperwork.adapters.NotesAdapter.Note;
import rocks.paperwork.network.SyncNotesTask;

public class NoteActivity extends AppCompatActivity
{
    private final String LOG_TAG = NoteActivity.class.getName();
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


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

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

            String content = parseNoteText(mNote.getContent());
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
            getWindow().setStatusBarColor(getResources().getColor(R.color.primaryDarkColor));
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
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            mEditContent.setFocusableInTouchMode(true);
            mEditContent.setCursorVisible(true);
            mTextTitle.setFocusableInTouchMode(true);
            mTextTitle.setCursorVisible(true);
        }
        else
        {
            mEditContent.setFocusable(false);
            mEditContent.setCursorVisible(false);

            mTextTitle.setFocusable(false);
            mTextTitle.setCursorVisible(false);
        }

        mEditMode = editMode;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        menu.findItem(R.id.action_save).setVisible(mEditMode);
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
            setResult(RESULT_CANCELED);
            onBackPressed();
            return true;
        }
        else if (id == R.id.action_save)
        {
            //NotesDataSource.getInstance(this).deleteNote(mNote);
            setNoteResult();
            onBackPressed();
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

    private boolean changesToSave()
    {
        Editable content = mEditContent.getText();
        Editable title = mTextTitle.getText();

        if (mNote == null) // note does not exist yet
        {
            if (content.length() != 0 || title.length() != 0)
            {
                return true;
            }
        }
        else
        {
            if (!content.toString().equals(mNote.getContent()) || !title.toString().equals(mNote.getTitle()))
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

    private String parseNoteText(String text)
    {
        text = text.replace("\\", "");

        return text;
    }
}
